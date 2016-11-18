package net.clgd.ccemux.emulation

import dan200.computercraft.core.computer.Computer
import dan200.computercraft.core.filesystem.FileMount
import java.io.File
import java.nio.file.Path
import java.util.ArrayList
import net.clgd.ccemux.Config
import org.eclipse.xtend.lib.annotations.Accessors
import org.slf4j.Logger

class CCEmuX(val logger: Logger, val conf: Config) : Runnable {
	val dataDir: Path = conf.dataDir
	val ccJar: File = dataDir.resolve(conf.CCLocal).toFile

	var env = EmulatedEnvironment(this)

	var running: Boolean = false
	var timeStarted: Long = 0L

	val computers = ArrayList<EmulatedComputer>()

	fun createEmulatedComputer(saveDir: Path): EmulatedComputer {
		val comp = createEmulatedComputer()
		logger.debug("Overriding save dir for computer {} to '{}'", comp.getID(), saveDir.toString())
		val field = Computer::class.java.getDeclaredField("m_rootMount")
		field.isAccessible = true
		field.set(comp.ccComputer, FileMount(saveDir.toFile(), env.computerSpaceLimit))
	}

	fun createEmulatedComputer(id: Int): EmulatedComputer {
		logger.trace("Creating emulated computer")
		synchronized(computers) {
			val comp = EmulatedComputer(this, conf.termWidth, conf.termHeight, id)
			computers.add(comp)
			logger.info("Created emulated computer ID {}", comp.getID())
			return comp
		}
	}

	fun createEmulatedComputer(): EmulatedComputer {
		createEmulatedComputer(-1)
	}

	fun removeEmulatedComputer(ec: EmulatedComputer) {
		synchronized(computers) {
			if (computers.contains(ec)) {
				logger.trace("Removing emulated computer ID {}", ec.ID)
				val success = computers.remove(ec)

				if (computers.isEmpty()) {
					running = false
					logger.info("All emulated computers removed, stopping event loop")
				}

				return success
			} else {
				return false
			}
		}
	}

	fun getTimeStartedInSeconds() = timeStarted / 1000.0f

	fun getTimeStartedInTicks() = getTimeStartedInSeconds() * 20.0f

	fun getTimeSinceStart() = System.currentTimeMillis() - timeStarted

	fun getSecondsSinceStart() = getTimeSinceStart() / 1000.0f

	fun getTicksSinceStart() = (getSecondsSinceStart() * 20).toInt()

	private fun update(dt: Float) {
		synchronized(computers) {
			computers.forEach { comp -> synchronized(comp, { comp.update(dt) }) }
		}
	}

	override fun run() {
		running = true

		timeStarted = System.currentTimeMillis()
		var lastTime = System.currentTimeMillis()

		while (running) {
			val now = System.currentTimeMillis()
			val dt = now - lastTime
			val dtSecs = dt / 1000.0f

			update(dtSecs)

			lastTime = System.currentTimeMillis()

			// ComputerCraft only needs to update 20 times a second.
			Thread.sleep(1000 / 20)
		}

		logger.debug("Emulation stopped")
	}
}