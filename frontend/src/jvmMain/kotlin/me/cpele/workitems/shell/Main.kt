package me.cpele.workitems.shell

import me.cpele.workitems.core.programs.Accounts
import me.cpele.workitems.core.programs.SlackAccount
import me.cpele.workitems.core.programs.WorkItems

const val MAIN_COMMAND_USAGE = "Usage: work-items <program-name> <program-command>"

fun main(vararg args: String): Unit = args.firstOrNull()
    ?.let { programArg ->
        when (programArg) {
            "slack-account" -> SlackAccount::main
            "accounts" -> Accounts::main
            "work-items" -> WorkItems::main
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
