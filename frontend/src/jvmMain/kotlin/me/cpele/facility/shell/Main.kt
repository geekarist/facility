package me.cpele.facility.shell

import me.cpele.facility.core.programs.SlackAccount

const val MAIN_COMMAND_USAGE = "Usage: facility <program-name> <program-command>"

fun main(vararg args: String): Unit = args.firstOrNull()
    ?.let { programArg ->
        when (programArg) {
            "slack-account" -> SlackAccount::main
            else -> {
                { System.err.println(MAIN_COMMAND_USAGE) }
            }
        }
    }?.let { programMainFun ->
        val programArgs: Array<String> = args.drop(1).toTypedArray()
        programMainFun to programArgs
    }
    ?.let { (programMainFun, args) -> programMainFun(args) }
    ?: run { System.err.println(MAIN_COMMAND_USAGE) }
