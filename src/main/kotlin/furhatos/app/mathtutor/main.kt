package furhatos.app.mathtutor

import Idle
import furhatos.flow.kotlin.Flow
import furhatos.skills.Skill

class MathTutorSkill : Skill() {
    override fun start() {
        Flow().run(Idle)
    }
}

fun main(args: Array<String>) {
    Skill.main(args)
}
