/**
 * Copyright (c) 2002-2013 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.bench.domain;

public final class Units
{
    // Neo concepts

    /**
     * A basic write transaction that uses the core API basic functions.
     */
    public static Unit CORE_API_WRITE_TRANSACTION = new Unit( "Core API write tx" );

    /**
     * This represents a non-transactional read operation
     * using the core API and indexes.
     * It is used by {@link org.neo4j.bench.cases.mixedload.MixedLoadBenchCase}.
     */
    public static Unit CORE_API_READ = new Unit( "Core API read" );

    // Time units

    public static Unit SECOND = new Unit("s");
    public static Unit MILLISECOND = new Unit("ms");

    // Other

    public static Unit UNKNOWN = new Unit( "unknown" );

    private Units(){};
}
