import lib.log.Logger
import java.net.*
import java.io.*

val log = Logger()

class SocketProxy() {
	fun handleConn(conn: Socket) {
		val ins = conn.getInputStream()
		val ous = conn.getOutputStream()

		val connProxy = Socket()
		connProxy.connect(InetSocketAddress(InetAddress.getByName("127.0.0.1"), 80))
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
//						close()
					} catch (e: java.lang.Exception) {
						e.printStackTrace()
					}
				}

				try {
					ins.close()
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
					flag = false
					close()
					return@Thread
				}
				if (length < 1) {
					log.d("EOF")
					flag = false
					close()
					break
				}
				log.d("request -> ${ String(request, 0, length) }")
				// request write to proxyConn
				try {
					proxyOus.write(request, 0, length)
				} catch (e: java.lang.Exception) {
					flag = false
					close()
				}
			}

			log.d("proxy thread ending")
		}.start()

		while (flag) {
			fun close() {
                try {
					proxyIns.close()
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
//					    close()
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
				log.d("response EOF")
				flag = false
				close()
				break
			}

			if (length < 1) {
				// what happened
				log.d("response < 1 -> $length")
				continue
			}
			log.d("response完毕 -> ${String(response, 0, length)}")
			try {
				ous.write(response, 0, length)
			} catch (e: java.lang.Exception) {
				flag = false
				close()
				break
			}
		}
	}
}

fun main(args: Array<String>) {
	val proxy = SocketProxy()
	val port = 8888
	val sock = ServerSocket(port)

	log.d("监听于端口$port")

	while (true) {
		val conn = sock.accept()
		log.d("客户端：$conn")
		Thread {
			try {
				proxy.handleConn(conn)
			} catch(e: Exception) {
				log.d(e.message ?: "Unknown error")
			}

			try {
				conn.close()
			} catch(e: Exception) {
				log.d("${ e.message }")
			}
		}.start()
	}
}

