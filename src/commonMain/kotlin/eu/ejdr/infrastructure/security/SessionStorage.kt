package eu.ejdr.infrastructure.security

interface SessionStorage {
    fun load(): String?
    fun save(value: String)
    fun clear()
    fun exists(): Boolean
}
