package teksturepako.pakkupro.io

import java.net.*
import java.util.*

enum class AddressType
{
    IP, MAC;
}

fun getNetworkAddress(addressType: AddressType): String?
{
    var address: String? = ""
    var lanIp: InetAddress? = null

    try
    {
        var ipAddress: String?
        val net: Enumeration<NetworkInterface>? = NetworkInterface.getNetworkInterfaces()

        if (net != null)
        {
            while (net.hasMoreElements())
            {
                val element = net.nextElement()
                val addresses = element.inetAddresses

                while (addresses.hasMoreElements()
                    && element.hardwareAddress?.isNotEmpty() == true
                    && !isVMMac(element.hardwareAddress)
                )
                {
                    val ip = addresses.nextElement()
                    if (ip is Inet4Address)
                    {
                        if (ip.isSiteLocalAddress())
                        {
                            ipAddress = ip.getHostAddress()
                            lanIp = InetAddress.getByName(ipAddress)
                        }
                    }
                }
            }
        }

        if (lanIp == null) return null

        address = when (addressType)
        {
            AddressType.IP  ->
            {
                lanIp.toString().replace("^/+".toRegex(), "")
            }
            AddressType.MAC ->
            {
                getMacAddress(lanIp)
            }
        }
    }
    catch (ex: UnknownHostException)
    {
        ex.printStackTrace()
    }
    catch (ex: SocketException)
    {
        ex.printStackTrace()
    }
    catch (ex: Exception)
    {
        ex.printStackTrace()
    }

    return address
}

private fun getMacAddress(ip: InetAddress): String?
{
    var address: String? = null

    try
    {
        val network = NetworkInterface.getByInetAddress(ip)
        val mac = network.hardwareAddress

        val sb = StringBuilder()
        for (i in mac.indices)
        {
            sb.append(String.format("%02X%s", mac[i], if ((i < mac.size - 1)) "-" else ""))
        }
        address = sb.toString()
    }
    catch (ex: SocketException)
    {
        ex.printStackTrace()
    }

    return address
}

private fun isVMMac(mac: ByteArray?): Boolean
{
    if (null == mac) return false

    val invalidMacs = arrayOf(
        byteArrayOf(0x00, 0x05, 0x69),          //VMWare
        byteArrayOf(0x00, 0x1C, 0x14),          //VMWare
        byteArrayOf(0x00, 0x0C, 0x29),          //VMWare
        byteArrayOf(0x00, 0x50, 0x56),          //VMWare
        byteArrayOf(0x08, 0x00, 0x27),          //Virtualbox
        byteArrayOf(0x0A, 0x00, 0x27),          //Virtualbox
        byteArrayOf(0x00, 0x03, 0xFF.toByte()), //Virtual-PC
        byteArrayOf(0x00, 0x15, 0x5D)           //Hyper-V
    )

    for (invalid in invalidMacs)
    {
        if (invalid[0] == mac[0] && invalid[1] == mac[1] && invalid[2] == mac[2]) return true
    }

    return false
}