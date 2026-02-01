package com.james.anvil.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import com.james.anvil.MainActivity
import com.james.anvil.R
import com.james.anvil.data.AnvilDatabase
import com.james.anvil.data.BlockedLink
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * A local VPN service that intercepts DNS requests and blocks domains
 * based on the user's blocklist. This provides network-level blocking
 * that works even in incognito mode and non-browser apps.
 *
 * Key Design:
 * - Creates a TUN interface that captures DNS traffic only
 * - Uses a local DNS proxy to filter blocked domains
 * - Blocked domains receive NXDOMAIN response
 * - Non-DNS traffic bypasses the VPN entirely
 * - Does NOT slow down internet (DNS-only filtering)
 */
class AnvilVpnService : VpnService() {

    companion object {
        const val ACTION_START = "com.james.anvil.vpn.START"
        const val ACTION_STOP = "com.james.anvil.vpn.STOP"

        private const val VPN_ADDRESS = "10.0.0.2"
        private const val VPN_DNS = "10.0.0.1" // Our local DNS proxy
        private const val UPSTREAM_DNS = "8.8.8.8" // Google DNS as upstream
        private const val VPN_MTU = 1500

        private const val NOTIFICATION_CHANNEL_ID = "anvil_vpn_channel"
        private const val NOTIFICATION_ID = 2001

        private val _isRunning = AtomicBoolean(false)
        
        val isRunning: Boolean
            get() = _isRunning.get()
    }

    private var vpnInterface: ParcelFileDescriptor? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Cache of blocked link patterns with schedule info
    private val blockedLinksMap = ConcurrentHashMap<String, BlockedLink>()

    @Volatile
    private var shouldRun = false

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        // Observe blocklist changes
        val db = AnvilDatabase.getDatabase(applicationContext)
        serviceScope.launch {
            db.blocklistDao().observeEnabledBlockedLinksWithSchedule().collectLatest { links ->
                blockedLinksMap.clear()
                links.forEach { link ->
                    blockedLinksMap[link.pattern.lowercase()] = link
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopVpn()
                return START_NOT_STICKY
            }
            ACTION_START, null -> {
                startVpn()
            }
        }
        return START_STICKY
    }

    private fun startVpn() {
        if (_isRunning.get()) return

        try {
            // Build the VPN interface - DNS only routing
            val builder = Builder()
                .setSession("ANVIL Blocker")
                .setMtu(VPN_MTU)
                .addAddress(VPN_ADDRESS, 24)
                .addDnsServer(VPN_DNS)
                // Only route DNS traffic through VPN (port 53)
                // By adding specific routes only for DNS, other traffic bypasses VPN
                .addRoute(VPN_DNS, 32)

            // Exclude our own app from VPN to prevent loops
            builder.addDisallowedApplication(packageName)

            vpnInterface = builder.establish()

            if (vpnInterface == null) {
                stopSelf()
                return
            }

            shouldRun = true
            _isRunning.set(true)

            // Start foreground service with notification (with type for Android 14+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    NOTIFICATION_ID, 
                    createNotification(),
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            } else {
                startForeground(NOTIFICATION_ID, createNotification())
            }

            // Start DNS proxy in background
            serviceScope.launch {
                runDnsProxy()
            }

        } catch (e: Exception) {
            e.printStackTrace()
            stopVpn()
        }
    }

    private fun stopVpn() {
        shouldRun = false
        _isRunning.set(false)

        try {
            vpnInterface?.close()
        } catch (e: Exception) {
            // Ignore close errors
        }
        vpnInterface = null

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    /**
     * Simple DNS proxy that runs on the VPN's DNS address.
     * Intercepts DNS queries, checks against blocklist, and either:
     * - Returns NXDOMAIN for blocked domains
     * - Forwards to upstream DNS for allowed domains
     */
    private fun runDnsProxy() {
        val vpnFd = vpnInterface ?: return
        val inputStream = FileInputStream(vpnFd.fileDescriptor)
        val outputStream = FileOutputStream(vpnFd.fileDescriptor)
        
        val buffer = ByteArray(VPN_MTU)

        try {
            while (shouldRun) {
                val length = inputStream.read(buffer)
                if (length <= 0) {
                    Thread.sleep(10)
                    continue
                }

                // Process packet
                serviceScope.launch {
                    try {
                        processPacket(buffer, length, outputStream)
                    } catch (e: Exception) {
                        // Ignore individual packet errors
                    }
                }
            }
        } catch (e: Exception) {
            if (shouldRun) {
                e.printStackTrace()
            }
        } finally {
            try {
                inputStream.close()
                outputStream.close()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    private fun processPacket(buffer: ByteArray, length: Int, outputStream: FileOutputStream) {
        if (length < 28) return // Too small for IP + UDP

        val packet = ByteBuffer.wrap(buffer, 0, length)
        
        // Check IP version
        val versionIhl = packet.get(0).toInt() and 0xFF
        val version = versionIhl shr 4
        if (version != 4) return // Only IPv4

        val ihl = (versionIhl and 0x0F) * 4
        if (length < ihl + 8) return

        // Check protocol - 17 = UDP
        val protocol = packet.get(9).toInt() and 0xFF
        if (protocol != 17) return

        // Check if it's DNS (port 53)
        val destPort = ((packet.get(ihl + 2).toInt() and 0xFF) shl 8) or (packet.get(ihl + 3).toInt() and 0xFF)
        if (destPort != 53) return

        // Extract DNS query
        val dnsStart = ihl + 8
        if (length < dnsStart + 12) return

        // Extract domain from DNS query
        val domain = extractDomain(buffer, dnsStart + 12, length)

        if (domain != null && shouldBlockDomain(domain)) {
            // Send NXDOMAIN response
            sendNxdomainResponse(buffer, length, ihl, dnsStart, outputStream)
        } else {
            // Forward to real DNS
            forwardDnsQuery(buffer, length, ihl, dnsStart, outputStream)
        }
    }

    private fun extractDomain(buffer: ByteArray, offset: Int, length: Int): String? {
        val sb = StringBuilder()
        var pos = offset

        try {
            while (pos < length) {
                val labelLen = buffer[pos].toInt() and 0xFF
                if (labelLen == 0) break
                if (labelLen > 63 || pos + labelLen >= length) return null

                if (sb.isNotEmpty()) sb.append('.')
                for (i in 1..labelLen) {
                    sb.append((buffer[pos + i].toInt() and 0xFF).toChar())
                }
                pos += labelLen + 1
            }
        } catch (e: Exception) {
            return null
        }

        return if (sb.isNotEmpty()) sb.toString().lowercase() else null
    }

    private fun shouldBlockDomain(domain: String): Boolean {
        val normalizedDomain = normalizeDomain(domain)

        for ((pattern, blockedLink) in blockedLinksMap) {
            if (!blockedLink.isBlockingActiveNow()) continue

            val normalizedPattern = normalizeDomain(pattern)

            if (normalizedDomain == normalizedPattern ||
                normalizedDomain.endsWith(".$normalizedPattern") ||
                normalizedDomain.contains(normalizedPattern)) {
                return true
            }
        }
        return false
    }

    private fun normalizeDomain(domain: String): String {
        var normalized = domain.lowercase()
        val prefixes = listOf("www.", "m.", "mobile.", "amp.", "web.", "touch.")
        for (prefix in prefixes) {
            if (normalized.startsWith(prefix)) {
                normalized = normalized.removePrefix(prefix)
                break
            }
        }
        return normalized
    }

    private fun sendNxdomainResponse(
        buffer: ByteArray,
        length: Int,
        ihl: Int,
        dnsStart: Int,
        outputStream: FileOutputStream
    ) {
        val response = buffer.copyOf(length)

        // Swap IP addresses
        for (i in 0..3) {
            val temp = response[12 + i]
            response[12 + i] = response[16 + i]
            response[16 + i] = temp
        }

        // Swap UDP ports
        val srcPort0 = response[ihl]
        val srcPort1 = response[ihl + 1]
        response[ihl] = response[ihl + 2]
        response[ihl + 1] = response[ihl + 3]
        response[ihl + 2] = srcPort0
        response[ihl + 3] = srcPort1

        // Set DNS flags: QR=1 (response), RCODE=3 (NXDOMAIN)
        response[dnsStart + 2] = 0x81.toByte()
        response[dnsStart + 3] = 0x83.toByte()

        // Recalculate IP checksum
        response[10] = 0
        response[11] = 0
        var sum = 0
        var i = 0
        while (i < ihl) {
            sum += ((response[i].toInt() and 0xFF) shl 8) or (response[i + 1].toInt() and 0xFF)
            i += 2
        }
        while (sum shr 16 != 0) {
            sum = (sum and 0xFFFF) + (sum shr 16)
        }
        val checksum = sum.inv() and 0xFFFF
        response[10] = (checksum shr 8).toByte()
        response[11] = (checksum and 0xFF).toByte()

        // Clear UDP checksum (optional for IPv4)
        response[ihl + 6] = 0
        response[ihl + 7] = 0

        synchronized(outputStream) {
            outputStream.write(response, 0, length)
            outputStream.flush()
        }
    }

    private fun forwardDnsQuery(
        buffer: ByteArray,
        length: Int,
        ihl: Int,
        dnsStart: Int,
        outputStream: FileOutputStream
    ) {
        try {
            val dnsLength = length - dnsStart
            val dnsData = buffer.copyOfRange(dnsStart, length)

            // Create protected socket for upstream DNS
            val socket = DatagramSocket()
            protect(socket)

            val dnsServer = InetAddress.getByName(UPSTREAM_DNS)
            socket.send(DatagramPacket(dnsData, dnsData.size, dnsServer, 53))

            // Receive response
            val responseBuffer = ByteArray(512)
            val responsePacket = DatagramPacket(responseBuffer, responseBuffer.size)
            socket.soTimeout = 3000
            socket.receive(responsePacket)
            socket.close()

            // Build response packet
            val responseLen = ihl + 8 + responsePacket.length
            val response = ByteArray(responseLen)

            // Copy and modify IP header
            System.arraycopy(buffer, 0, response, 0, ihl)

            // Swap IP addresses
            for (i in 0..3) {
                val temp = response[12 + i]
                response[12 + i] = response[16 + i]
                response[16 + i] = temp
            }

            // Update IP total length
            response[2] = (responseLen shr 8).toByte()
            response[3] = (responseLen and 0xFF).toByte()

            // Recalculate IP checksum
            response[10] = 0
            response[11] = 0
            var sum = 0
            var i = 0
            while (i < ihl) {
                sum += ((response[i].toInt() and 0xFF) shl 8) or (response[i + 1].toInt() and 0xFF)
                i += 2
            }
            while (sum shr 16 != 0) {
                sum = (sum and 0xFFFF) + (sum shr 16)
            }
            val checksum = sum.inv() and 0xFFFF
            response[10] = (checksum shr 8).toByte()
            response[11] = (checksum and 0xFF).toByte()

            // Copy and modify UDP header
            System.arraycopy(buffer, ihl, response, ihl, 8)
            // Swap ports
            val srcPort0 = response[ihl]
            val srcPort1 = response[ihl + 1]
            response[ihl] = response[ihl + 2]
            response[ihl + 1] = response[ihl + 3]
            response[ihl + 2] = srcPort0
            response[ihl + 3] = srcPort1

            // Update UDP length
            val udpLen = 8 + responsePacket.length
            response[ihl + 4] = (udpLen shr 8).toByte()
            response[ihl + 5] = (udpLen and 0xFF).toByte()

            // Clear UDP checksum
            response[ihl + 6] = 0
            response[ihl + 7] = 0

            // Copy DNS response
            System.arraycopy(responsePacket.data, 0, response, ihl + 8, responsePacket.length)

            synchronized(outputStream) {
                outputStream.write(response, 0, responseLen)
                outputStream.flush()
            }

        } catch (e: Exception) {
            // Timeout or error - drop packet
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "ANVIL VPN Blocker",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows when ANVIL is actively blocking websites"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, AnvilVpnService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("ANVIL Blocking Active")
            .setContentText("Website blocking is enabled")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .addAction(R.drawable.ic_launcher_foreground, "Stop", stopPendingIntent)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        shouldRun = false
        _isRunning.set(false)
        serviceScope.cancel()
        try {
            vpnInterface?.close()
        } catch (e: Exception) {
            // Ignore
        }
    }

    override fun onRevoke() {
        super.onRevoke()
        stopVpn()
    }
}
