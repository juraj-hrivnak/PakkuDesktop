package teksturepako.pakkupro.io

import java.net.*

internal enum class AddressType { IP, MAC }

internal fun getNetworkAddress(addressType: AddressType): String? =
    getLocalInetAddress()?.let { inetAddress ->
        when (addressType) {
            AddressType.IP -> inetAddress.toString().replace("^/+".toRegex(), "")
            AddressType.MAC -> getMacAddress(inetAddress)
        }
    }

private fun getLocalInetAddress(): InetAddress? =
    runCatching {
        NetworkInterface.getNetworkInterfaces()
            .asSequence()
            .filter { it.hardwareAddress?.isNotEmpty() == true }
            .filter { !isVMMac(it.hardwareAddress) }
            .flatMap { networkInterface ->
                networkInterface.inetAddresses
                    .asSequence()
                    .filterIsInstance<Inet4Address>()
                    .filter { it.isSiteLocalAddress }
            }
            .firstOrNull()
            ?.hostAddress
            ?.let { InetAddress.getByName(it) }
    }.getOrNull()

private fun getMacAddress(ip: InetAddress): String? =
    runCatching {
        NetworkInterface.getByInetAddress(ip)
            .hardwareAddress
            .asSequence()
            .mapIndexed { index, byte ->
                String.format(
                    "%02X%s",
                    byte,
                    if (index < NetworkInterface.getByInetAddress(ip).hardwareAddress.size - 1) "-" else ""
                )
            }
            .joinToString("")
    }.getOrNull()

private fun isVMMac(mac: ByteArray?): Boolean {
    val invalidMacs = listOf(
        byteArrayOf(0x00, 0x05, 0x69),          // VMWare
        byteArrayOf(0x00, 0x1C, 0x14),          // VMWare
        byteArrayOf(0x00, 0x0C, 0x29),          // VMWare
        byteArrayOf(0x00, 0x50, 0x56),          // VMWare
        byteArrayOf(0x08, 0x00, 0x27),          // Virtualbox
        byteArrayOf(0x0A, 0x00, 0x27),          // Virtualbox
        byteArrayOf(0x00, 0x03, 0xFF.toByte()), // Virtual-PC
        byteArrayOf(0x00, 0x15, 0x5D)           // Hyper-V
    )

    return mac?.let { macArray ->
        invalidMacs.any { invalid ->
            invalid
                .take(3)
                .zip(macArray.take(3))
                .all { (a, b) -> a == b }
        }
    } ?: false
}
