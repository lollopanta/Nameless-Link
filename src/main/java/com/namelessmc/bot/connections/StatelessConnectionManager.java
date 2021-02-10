package com.namelessmc.bot.connections;

import com.namelessmc.bot.Main;
import com.namelessmc.java_api.NamelessAPI;
import org.apache.commons.lang3.Validate;

import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class StatelessConnectionManager extends ConnectionManager {

	private final long guildId;
	private final Optional<NamelessAPI> api; // Keep one instance for performance

	public StatelessConnectionManager(final long guildId, final URL apiUrl) {
		Validate.notNull(apiUrl, "API URL not specified");
		this.guildId = guildId;
		this.api = Optional.of(Main.newApiConnection(apiUrl));
	}

	@Override
	public Optional<NamelessAPI> getApi(final long guildId) {
		if (guildId != this.guildId) {
			return Optional.empty();
		} else {
			return this.api;
		}
	}

	@Override
	public boolean isReadOnly() {
		return true;
	}

	@Override
	public void newConnection(final long guildId, final URL apiUrl) throws BackendStorageException {
		throw new BackendStorageException(new UnsupportedOperationException());
	}

	@Override
	public boolean removeConnection(final long guildId) throws BackendStorageException {
		throw new BackendStorageException(new UnsupportedOperationException());
	}

	@Override
	public Optional<Long> getLastUsed(final long guildId) throws BackendStorageException {
		throw new BackendStorageException(new UnsupportedOperationException());
	}

	@Override
	public List<URL> listConnections() {
		// Optional should always be present, this method should never throw an exception here
		return Collections.singletonList(this.api.orElseThrow().getApiUrl());
	}

	@Override
	public boolean updateConnection(final long guildId, final URL apiUrl) throws BackendStorageException {
		throw new BackendStorageException(new UnsupportedOperationException());
	}

	@Override
	public List<URL> listConnectionsUsedBefore(final long time) throws BackendStorageException {
		throw new BackendStorageException(new UnsupportedOperationException());
	}

	@Override
	public List<URL> listConnectionsUsedSince(final long time) throws BackendStorageException {
		throw new BackendStorageException(new UnsupportedOperationException());
	}

	@Override
	public Optional<Long> getGuildIdByURL(final URL url) throws BackendStorageException {
		if (this.api.isPresent()) {
			if (url == this.api.get().getApiUrl()) {
				return Optional.of(this.guildId);
			} else {
				throw new BackendStorageException(new UnsupportedOperationException());
			}
		}
		return Optional.empty();
	}

}
