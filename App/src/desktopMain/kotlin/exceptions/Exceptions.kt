package exceptions

class LeagueNotFoundException(message: String) : Exception(message)
class ScriptException(val ex: Throwable? = null, override val message: String? = ex?.message) : Exception()