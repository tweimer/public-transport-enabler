/*
 * Copyright the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package de.schildbach.pte.provider;

import org.msgpack.core.MessageUnpacker;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;

import javax.annotation.Nullable;

import de.schildbach.pte.NetworkId;
import de.schildbach.pte.dto.JourneyRef;
import de.schildbach.pte.dto.Line;
import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.LocationType;
import de.schildbach.pte.dto.NearbyLocationsResult;
import de.schildbach.pte.dto.Product;
import de.schildbach.pte.dto.QueryDeparturesResult;
import de.schildbach.pte.dto.QueryJourneyResult;
import de.schildbach.pte.dto.QueryTripsContext;
import de.schildbach.pte.dto.QueryTripsResult;
import de.schildbach.pte.dto.QueryVehicleInformationResult;
import de.schildbach.pte.dto.Stop;
import de.schildbach.pte.dto.Style;
import de.schildbach.pte.dto.Trip;
import de.schildbach.pte.dto.TripOptions;
import de.schildbach.pte.dto.TripRef;
import de.schildbach.pte.dto.TripShare;
import de.schildbach.pte.provider.locationsearch.LocationSearchProvider;

/**
 * Interface to be implemented by providers of transportation networks.
 * 
 * @author Andreas Schildbach
 */
public interface NetworkProvider extends Provider, LocationSearchProvider {
    /**
     * Represents the capabilities of a network provider.
     */
    enum Capability {
        /** can suggest locations */
        SUGGEST_LOCATIONS,
        /** can determine nearby locations */
        NEARBY_LOCATIONS,
        /** can query for departures */
        DEPARTURES,
        /** can query for arrivals */
        ARRIVALS,
        /** can query trips */
        TRIPS,
        /** supports trip queries passing by a specific location */
        TRIPS_VIA,
        /** can query journeys */
        JOURNEY,
        /** can reload a trip */
        TRIP_RELOAD,
        /** supports minimum transfer times */
        MIN_TRANSFER_TIMES,
        /** supports bike option for trips */
        BIKE_OPTION,
        /** supports direct option for trips */
        DIRECT_OPTION,
        /** supports trip sharing */
        TRIP_SHARING,
        /** supports trip linking */
        TRIP_LINKING,
        /** can provide trip details */
        TRIP_DETAILS,
        /** can provide vehicle information */
        VEHICLE_INFORMATION,
    }

    /**
     * Represents trip optimization criteria.
     */
    enum Optimize {
        /** Minimize travel duration */
        LEAST_DURATION,
        /** Minimize number of transfers */
        LEAST_CHANGES,
        /** Minimize walking distance */
        LEAST_WALKING
    }

    /**
     * Represents walking speed preference for trip planning.
     */
    enum WalkSpeed {
        /** Slow walking pace */
        SLOW,
        /** Normal walking pace */
        NORMAL,
        /** Fast walking pace */
        FAST
    }

    /**
     * Represents accessibility requirements for trips.
     */
    enum Accessibility {
        /** No specific accessibility requirements */
        NEUTRAL,
        /** Limited accessibility (limited wheelchair access, etc.) */
        LIMITED,
        /** Fully barrier-free accessibility */
        BARRIER_FREE
    }

    /**
     * Represents flags that can be applied to trips.
     */
    enum TripFlag {
        /** Trip allows bike transport */
        BIKE,
        /** Direct trip without transfers */
        DIRECT
    }

    /**
     * Represents the types of trip details that can be queried.
     */
    enum TripDetails {
        /** Details about transfers in the trip */
        TRANSFERS
    }

    /**
     * Represents the mode for handling equivalent stations.
     */
    enum EquivalentStationsMode {
        /** Keep distinct stations separate */
        KEEP_DISTINCT,
        /** Merge to meta station if names are the same */
        META_IF_SAME_NAME,
        /** Use meta station for all equivalent stations */
        USE_META,
    }

    /**
     * Get the identifier of this network provider.
     * 
     * @return the network identifier
     */
    NetworkId id();

    /**
     * Check whether this network provider has the specified capabilities.
     * 
     * @param capabilities
     *            the capabilities to check
     * @return true if all specified capabilities are supported, false otherwise
     */
    boolean hasCapabilities(final Capability... capabilities);

    /**
     * Get the time zone of the network.
     * 
     * @return the time zone
     */
    TimeZone getTimeZone();

    /**
     * Check if this provider requires credentials for authentication.
     * 
     * @return true if credentials are required, false otherwise
     */
    boolean requiresCredentials();

    /**
     * Set credentials for authentication with the provider.
     * 
     * @param credentials
     *            the credentials to set
     */
    void setCredentials(String credentials);

    /**
     * Find locations near to given location. At least one of lat/lon pair or station id must be present in
     * that location.
     *
     * @param types
     *            types of locations to find
     * @param location
     *            location to determine nearby stations
     * @param equivsMode
     *            whether to return master (meta) stations, or each particular sub-station
     * @param maxDistance
     *            maximum distance in meters, or {@code 0}
     * @param maxLocations
     *            maximum number of locations, or {@code 0}
     * @param products
     *            filter to stations serving listed products, or {@code null}
     * @return nearby stations
     * @throws IOException
     *             if an I/O error occurs
     */
    NearbyLocationsResult queryNearbyLocations(
            Set<LocationType> types,
            Location location,
            EquivalentStationsMode equivsMode,
            int maxDistance,
            int maxLocations,
            Set<Product> products) throws IOException;

    /**
     * Get departures/arrivals at a given station, probably live
     *
     * @param stationId
     *            id of the station
     * @param time
     *            desired time for departing, or {@code null} for the provider default
     * @param arrivals
     *            true to get arrivals, false to get departures
     * @param maxEvents
     *            maximum number of events to get or {@code 0}
     * @param equivsMode
     *            how to handle equivalent stations
     * @param products
     *            filter to stations serving listed products, or {@code null}
     * @return result object containing the departures
     * @throws IOException
     *             if an I/O error occurs
     */
    QueryDeparturesResult queryStationBoard(
            String stationId,
            @Nullable Date time,
            boolean arrivals,
            int maxEvents,
            EquivalentStationsMode equivsMode,
            Set<Product> products) throws IOException;

    /**
     * Get departures at a given station, probably live
     * (legacy interface for queryStationBoard with arrivals=false)
     *
     * @param stationId
     *            id of the station
     * @param time
     *            desired time for departing, or {@code null} for the provider default
     * @param maxDepartures
     *            maximum number of departures to get or {@code 0}
     * @param equivsMode
     *            how to handle equivalent stations
     * @param products
     *            filter to stations serving listed products, or {@code null}
     * @return result object containing the departures
     * @throws IOException
     *             if an I/O error occurs
     */
    QueryDeparturesResult queryDepartures(
            String stationId,
            @Nullable Date time,
            int maxDepartures,
            EquivalentStationsMode equivsMode,
            Set<Product> products) throws IOException;

    /**
     * Get the typical products served by this network.
     * 
     * @return set of default products
     */
    Set<Product> defaultProducts();

    /**
     * Query trips, asking for any ambiguousnesses
     * 
     * @param from
     *            location to route from, mandatory
     * @param via
     *            location to route via, may be {@code null}
     * @param to
     *            location to route to, mandatory
     * @param date
     *            desired date for departing, mandatory
     * @param dep
     *            date is departure date? {@code true} for departure, {@code false} for arrival
     * @param options
     *            additional trip options such as products, optimize, walkSpeed and accessibility, or
     *            {@code null} for the provider default
     * @param loadPath
     *            whether to load the full path information for trips
     * @return result object that can contain alternatives to clear up ambiguousnesses, or contains possible
     *         trips
     * @throws IOException
     *             if an I/O error occurs
     */
    QueryTripsResult queryTrips(
            Location from, @Nullable Location via, Location to, Date date, boolean dep,
            @Nullable TripOptions options,
            boolean loadPath) throws IOException;

    /**
     * Query trips with detailed options (deprecated).
     * 
     * @param from
     *            location to route from, mandatory
     * @param via
     *            location to route via, may be {@code null}
     * @param to
     *            location to route to, mandatory
     * @param date
     *            desired date for departing, mandatory
     * @param dep
     *            date is departure date? {@code true} for departure, {@code false} for arrival
     * @param products
     *            filter products, or {@code null} for the provider default
     * @param optimize
     *            optimization criteria, or {@code null} for the provider default
     * @param walkSpeed
     *            walking speed preference, or {@code null} for the provider default
     * @param accessibility
     *            accessibility requirements, or {@code null} for the provider default
     * @param flags
     *            trip flags, or {@code null} for no flags
     * @param loadPath
     *            whether to load the full path information for trips
     * @return result object that can contain alternatives to clear up ambiguousnesses, or contains possible
     *         trips
     * @throws IOException
     *             if an I/O error occurs
     * @deprecated use {@link #queryTrips(Location, Location, Location, Date, boolean, TripOptions, boolean)} instead
     */
    @Deprecated
    QueryTripsResult queryTrips(
            Location from, @Nullable Location via, Location to, Date date, boolean dep,
            @Nullable Set<Product> products, @Nullable Optimize optimize, @Nullable WalkSpeed walkSpeed,
            @Nullable Accessibility accessibility, @Nullable Set<TripFlag> flags,
            boolean loadPath) throws IOException;

    /**
     * Query more trips (e.g. earlier or later)
     * 
     * @param context
     *            context to query more trips from
     * @param later
     *            {@code true} to get later trips, {@code false} to get earlier trips
     * @param loadPath
     *            whether to load the full path information for trips
     * @return result object that contains possible trips
     * @throws IOException
     *             if an I/O error occurs
     */
    QueryTripsResult queryMoreTrips(QueryTripsContext context, boolean later, boolean loadPath) throws IOException;

    /**
     * Reload a trip based on a trip reference.
     * 
     * @param tripRef
     *            reference to the trip to reload
     * @param loadPath
     *            whether to load the full path information for the trip
     * @return result object containing the reloaded trip
     * @throws IOException
     *             if an I/O error occurs
     */
    QueryTripsResult queryReloadTrip(TripRef tripRef, boolean loadPath) throws IOException;

    /**
     * Query journey details based on a journey reference.
     * 
     * @param journeyRef
     *            reference to the journey
     * @param splitSubJourneys
     *            whether to split sub-journeys
     * @param loadPath
     *            whether to load the full path information
     * @return result object containing journey information
     * @throws IOException
     *             if an I/O error occurs
     */
    QueryJourneyResult queryJourney(JourneyRef journeyRef, boolean splitSubJourneys, boolean loadPath) throws IOException;

    /**
     * Query vehicle information for a specific journey and stop.
     * 
     * @param journeyRef
     *            reference to the journey
     * @param stop
     *            the stop at which to get vehicle information
     * @return result object containing vehicle information
     * @throws IOException
     *             if an I/O error occurs
     */
    QueryVehicleInformationResult queryVehicleInformation(JourneyRef journeyRef, Stop stop) throws IOException;

    /**
     * Check if vehicle information may be provided for the given journey and line.
     * 
     * @param journeyRef
     *            reference to the journey
     * @param line
     *            the line to check
     * @return true if vehicle information may be provided, false otherwise
     */
    boolean mayProvideVehicleInformation(JourneyRef journeyRef, Line line);

    /**
     * Get style of line.
     * 
     * @param network
     *            network to disambiguate line, may be {@code null}
     * @param product
     *            line product to get style of, may be {@code null}
     * @param label
     *            line label to get style of, may be {@code null}
     * @param styleFromNetwork
     *            style delivered by network, may be {@code null}
     * @return object containing background, foreground and optional border colors
     */
    Style lineStyle(
            @Nullable String network,
            @Nullable Product product,
            @Nullable String label,
            final @Nullable Style styleFromNetwork);

    /**
     * Unpack a trip reference from a MessagePack message.
     * 
     * @param unpacker
     *            the MessageUnpacker to read from
     * @return the unpacked trip reference
     * @throws IOException
     *             if an I/O error occurs
     */
    TripRef unpackTripRefFromMessage(final MessageUnpacker unpacker) throws IOException;

    /**
     * Unpack trip sharing information from a MessagePack message.
     * 
     * @param unpacker
     *            the MessageUnpacker to read from
     * @return the unpacked trip share information
     * @throws IOException
     *             if an I/O error occurs
     */
    TripShare unpackTripShareFromMessage(final MessageUnpacker unpacker) throws IOException;

    /**
     * Get an open/view link for the given trip.
     * 
     * @param trip
     *            the trip to get a link for
     * @return a URL that opens the trip in the network provider's web interface
     * @throws IOException
     *             if an I/O error occurs
     */
    String getOpenLink(final Trip trip) throws IOException;

    /**
     * Get a shareable link for the given trip.
     * 
     * @param trip
     *            the trip to get a link for
     * @return a URL that can be shared to view the trip
     * @throws IOException
     *             if an I/O error occurs
     */
    String getShareLink(final Trip trip) throws IOException;

    /**
     * Create a share of the given trip.
     * 
     * @param trip
     *            the trip to share
     * @return trip share information including sharing details
     * @throws IOException
     *             if an I/O error occurs
     */
    TripShare shareTrip(final Trip trip) throws IOException;

    /**
     * Extract trip share information from a shared text message.
     * 
     * @param textMessage
     *            the text message containing shared trip information
     * @return trip share information extracted from the message
     * @throws IOException
     *             if an I/O error occurs
     */
    TripShare getTripShareFromSharedTextMessage(final String textMessage) throws IOException;

    /**
     * Load a trip that was previously shared.
     * 
     * @param tripShare
     *            the shared trip information
     * @param loadPath
     *            whether to load the full path information
     * @return result object containing the loaded trip
     * @throws IOException
     *             if an I/O error occurs
     */
    QueryTripsResult loadSharedTrip(final TripShare tripShare, final boolean loadPath) throws IOException;

    /**
     * Create a new trip reference with updated legs and destination from a previous trip.
     * 
     * @param trip
     *            the original trip
     * @param newLegs
     *            the new legs for the trip
     * @param newTo
     *            the new destination location
     * @return a new trip reference with the updated information
     */
    TripRef createTripRefFromPreviousTripWithNewLegs(final Trip trip, final List<Trip.Leg> newLegs, final Location newTo);

    /**
     * Query detailed information for a trip.
     * 
     * @param trip
     *            the trip to get details for
     * @param whichDetails
     *            which types of details to retrieve
     * @return the trip with additional details filled in
     * @throws IOException
     *             if an I/O error occurs
     */
    Trip queryTripDetails(final Trip trip, final List<TripDetails> whichDetails) throws IOException;

    /**
     * Get the transfer evaluation provider for this network.
     * 
     * @return a TransferEvaluationProvider instance
     * @throws IOException
     *             if an I/O error occurs
     */
    TransferEvaluationProvider getTransferEvaluationProvider() throws IOException;

    /**
     * Get an informational URL for a location.
     * 
     * @param location
     *            the location to get information for
     * @return a URL with information about the location, or {@code null} if not available
     */
    String getLocationInfoUrl(Location location);
}
