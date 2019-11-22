import lib.config.JsonConfig
import lib.log.Logger
import java.net.*
import java.io.*

val log = Logger()
val config = JsonConfig("config.json")

class SocketProxy() {
	fun handleConn(conn: Socket) {
		val ins = BufferedInputStream(conn.getInputStream())
		val ous = conn.getOutputStream()

		// HTTP protocol
        var buf = ByteArray(1024)
		ins.mark(1024)
		val testSize = ins.read(buf)
		ins.reset()
		val test = try {
			String(buf, 0, testSize).split("""(\r\n|\n)""".toRegex(), 2)[0]
		} catch (e: java.lang.Exception) { "" }

		val regex = """(?:GET|HEAD|POST|PUT|DELETE).*""".toRegex()
		val proxyAddress =
			if (regex.matches(test)) {
                log.d("HTTP代理")
				InetSocketAddress(Inet4Address.getByName(config.get("http_address")), config.get("http_port").toInt())
			} else {
				log.d("Not HTTP代理")
				InetSocketAddress(Inet4Address.getByName(config.get("proxy_address")), config.get("proxy_port").toInt())
			}

		val connProxy = Socket()
		connProxy.connect(proxyAddress)

		val proxyIns = connProxy.getInputStream()
		val proxyOus = connProxy.getOutputStream()

		// 同步
		var flag = true

		// 从客户端读数据，写入代理
		Thread {
            fun close() {
                with (proxyOus) {
					try {
						flush()
					} catch (e: java.lang.Exception) {
						e.printStackTrace()
					}
                    try {
                        connProxy.shutdownOutput()
					} catch (e: java.lang.Exception) {
						e.printStackTrace()
					}
				}

				try {
					conn.shutdownInput()
				} catch (e: java.lang.Exception) {
					e.printStackTrace()
				}
			}

			while (flag) {
				log.d("Waiting for request data")
				val request = ByteArray(ins.available() + 10240)
				val length = try {
					ins.read(request) // 此处堵塞
				} catch (e: java.lang.Exception) {
					log.d("conn输入流错误")
					flag = false
					close()
					return@Thread
				}

				if (length < 1) {
                    log.d("conn输入流关机")
					close()
					break
				}

//				log.d("request -> ${ String(request, 0, length) }")
				// request write to proxyConn
				try {
					proxyOus.write(request, 0, length)
				} catch (e: java.lang.Exception) {
					flag = false
					close()
				}
			}

			log.d("proxy request thread ending")
		}.start()

		// 等待connProxy的response，写入conn
		while (flag) {
			fun close() {
                try {
					connProxy.shutdownInput()
				} catch (e: java.lang.Exception) {
					e.printStackTrace()
				}

				with(ous) {
					try {
					    flush()
					} catch (e: java.lang.Exception) {
						e.printStackTrace()
					}
					try {
                        conn.shutdownOutput()
					} catch (e: java.lang.Exception) {
						e.printStackTrace()
					}
				}
			}

			log.d("Waiting for response")
			val response = ByteArray(proxyIns.available() + 10240)
			val length = try {
				proxyIns.read(response)
			} catch (e: java.lang.Exception) {
				log.d("connProxy输入流错误")
				flag = false
				close()
				break
			}

			if (length < 1) {
				log.d("connProxy输入流关机")
                close()
                break
			}

//			log.d("response完毕 -> ${String(response, 0, length)}")

			try {
				ous.write(response, 0, length)
			} catch (e: java.lang.Exception) {
				log.d("conn输出流错误")
				flag = false
				close()
				break
			}
		}

		// 关闭connProxy
        if (false)
		try {
		    connProxy.close()
		} catch (e: java.lang.Exception) {
			log.d("关闭connProxy时发生了错误，${ e.message }")
		}

		log.d("proxy response thread ending")
	}
}

fun main(args: Array<String>) {
	val proxy = SocketProxy()
	val listenAddress = InetAddress.getByName(config.get("listen_address"))
	val listenPort = config.get("listen_port").toInt()
	val sock = ServerSocket(listenPort, 0, listenAddress)

	log.d("监听于$listenAddress : $listenPort")

	while (true) {
		val conn = sock.accept()
		log.d("客户端：$conn")
		Thread {
			try {
				proxy.handleConn(conn)
			} catch(e: Exception) {
				log.d("处理连接时发生了异常：${ e.message ?: "Unknown error" }")
			}

			try {
				conn.close()
			} catch(e: Exception) {
				log.d("关闭 $conn 时发生了异常：${ e.message }")
			}
		}.start()
	}
}

