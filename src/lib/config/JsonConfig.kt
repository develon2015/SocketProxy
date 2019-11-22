package lib.config

import log
import java.io.File

class JsonConfig(file: File) {

    private val global = HashMap<String, String>()

    constructor(fileName: String) : this(File(fileName))

    init {
        var content: String = String(file.readBytes(), Charsets.UTF_8)
        //content = content.replace("""(\r|\n|\t)""".toRegex(), "")
        log.d(content)

        val regex = """"(.+)":\s*("(?:.+)"|(?:\d+))""".toRegex()
        val r = regex.findAll(content)
        r.forEachIndexed { index, it ->
//            println("大分组$index: ${ it.value }")
//            it.groupValues.forEachIndexed { index, it ->
//                println("$index -> ${ it }")
//            }
            var (key, value) = it.destructured
            with (""""(.*)"""".toRegex().matchEntire(value)) {
                if (this != null) {
                    value = groups.get(1) ?. value ?: value
                }
            }

            global.put(key, value)
        }
    }

    fun get(key: String) = global.get(key) ?: "Unknown"

}