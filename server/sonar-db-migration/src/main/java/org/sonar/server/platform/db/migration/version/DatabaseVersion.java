/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.platform.db.migration.version;

import com.google.common.annotations.VisibleForTesting;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import org.sonar.db.DatabaseUtils;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;

public class DatabaseVersion {

  public static final int LAST_VERSION = 1_502;

  /**
   * The minimum supported version which can be upgraded. Lower
   * versions must be previously upgraded to LTS version.
   * Note that the value can't be less than current LTS version.
   */
  public static final int MIN_UPGRADE_VERSION = 1_152;

  private final DbClient dbClient;

  public DatabaseVersion(DbClient dbClient) {
    this.dbClient = dbClient;
  }

  @VisibleForTesting
  static Status getStatus(@Nullable Integer currentVersion, int lastVersion) {
    Status status = Status.FRESH_INSTALL;
    if (currentVersion != null) {
      if (currentVersion == lastVersion) {
        status = Status.UP_TO_DATE;
      } else if (currentVersion > lastVersion) {
        status = Status.REQUIRES_DOWNGRADE;
      } else {
        status = Status.REQUIRES_UPGRADE;
      }
    }
    return status;
  }

  public Status getStatus() {
    return getStatus(getVersion(), LAST_VERSION);
  }

  public Integer getVersion() {
    try (DbSession dbSession = dbClient.openSession(false)) {
      if (!DatabaseUtils.tableExists("SCHEMA_MIGRATIONS", dbSession.getConnection())) {
        return null;
      }

      List<Integer> versions = dbClient.schemaMigrationDao().selectVersions(dbSession);
      if (!versions.isEmpty()) {
        Collections.sort(versions);
        return versions.get(versions.size() - 1);
      }
      return null;
    }
  }

  public enum Status {
    UP_TO_DATE, REQUIRES_UPGRADE, REQUIRES_DOWNGRADE, FRESH_INSTALL
  }
}