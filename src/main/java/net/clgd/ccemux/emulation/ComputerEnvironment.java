package net.clgd.ccemux.emulation;

import java.io.*;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.filesystem.IMount;
import dan200.computercraft.api.filesystem.IWritableMount;
import dan200.computercraft.core.computer.IComputerEnvironment;
import dan200.computercraft.core.filesystem.ComboMount;
import dan200.computercraft.core.filesystem.FileMount;
import dan200.computercraft.core.filesystem.JarMount;
import net.clgd.ccemux.api.emulation.filesystem.VirtualDirectory;
import net.clgd.ccemux.api.emulation.filesystem.VirtualMount;

class ComputerEnvironment implements IComputerEnvironment {
	private static final Logger log = LoggerFactory.getLogger(ComputerEnvironment.class);

	private final CCEmuX emu;
	private final int id;
	private final Supplier<IWritableMount> mount;

	ComputerEnvironment(CCEmuX emu, int id, Supplier<IWritableMount> mount) {
		this.emu = emu;
		this.id = id;
		this.mount = mount;
	}

	@Override
	public long getComputerSpaceLimit() {
		return emu.getConfig().maxComputerCapacity.get();
	}

	@Override
	public int getDay() {
		return (int) (((emu.getTicksSinceStart() + 6000) / 24000) + 1);
	}

	@Override
	public String getHostString() {
		String version = CCEmuX.getVersion();
		if (version != null) {
			return String.format("ComputerCraft %s (CCEmuX %s)", ComputerCraft.getVersion(), version);
		} else {
			return String.format("ComputerCraft %s (CCEmuX)", ComputerCraft.getVersion());
		}
	}

	@Override
	public int assignNewID() {
		return emu.assignNewID();
	}


	@Override
	public IMount createResourceMount(String domain, String subPath) {
		String path = Paths.get("assets", domain, subPath).toString().replace('\\', '/');
		if (path.startsWith("/")) path = path.substring(1);

		JarMount jarMount;
		try {
			jarMount = new JarMount(emu.getCCJar(), path);
		} catch (IOException e) {
			log.error("Could not create mount from mod jar", e);
			return null;
		}

		VirtualDirectory.Builder romBuilder = new VirtualDirectory.Builder();
		emu.getPluginMgr().onCreatingROM(emu, romBuilder);

		return new ComboMount(new IMount[] {
			// From ComputerCraft JAR
			jarMount,
			// From plugin files
			new VirtualMount(romBuilder.build()),
			// From data directory
			new FileMount(emu.getConfig().getAssetDir().resolve(path).toFile(), 0)
		});
	}

	@Override
	public InputStream createResourceFile(String domain, String subPath) {
		String path = Paths.get("assets", domain, subPath).toString().replace('\\', '/');
		if (path.startsWith("/")) path = path.substring(1);

		File assetFile = emu.getConfig().getAssetDir().resolve(path).toFile();
		if (assetFile.exists() && assetFile.isFile()) {
			try {
				return new FileInputStream(assetFile);
			} catch (FileNotFoundException e) {
				log.error("Failed to create resource file", e);
			}
		}

		return CCEmuX.class.getClassLoader().getResourceAsStream(path);
	}

	@Override
	public IWritableMount createSaveDirMount(String path, long capacity) {
		// createSaveDirMount should only be called with computer/$id.
		if (!path.equals("computer/" + id)) {
			log.error("Unexpected call to createSaveDirMount for {}", capacity);
			return new FileMount(emu.getConfig().getComputerDir().resolve(path).toFile(), capacity);
		}

		return Optional.ofNullable(mount)
			.map(Supplier::get)
			.orElseGet(() -> new FileMount(emu.getConfig().getComputerDir().resolve(path).toFile(), capacity));
	}

	@Override
	public double getTimeOfDay() {
		return ((emu.getTicksSinceStart() + 6000) % 24000) / 1000d;
	}

	@Override
	public boolean isColour() {
		return true;
	}
}
