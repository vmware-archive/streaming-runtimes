# Building Sessions

<!-- NOTE: THIS IS NOT READY FOR PUBLISHING. IT NEEDS AN IMPLEMENTATION.
IN ADDITION, THERE ARE SPECIFIC SECTIONS MARKED WITH TODO COMMENTS. -->

Many real-world systems provide continuous data collection, but people are often
interested in breaking continuous data into related sessions. In addition to
online activities like building website activity sessions, vehicle or other IoT
sensors may benefit from building dynamic-length sessions based on usage.

In this example, we will build sessions (driving trips) from vehicle movement
logs. Note that these sessions are dynamic and may be subject to revision: for
example, if the vehicle comes to a stop for a few minutes with the engine off,
it may be the end of the trip, or it may be a short refueling stop.
Additionally, depending on the vehicle model, location and other information may
not be uploaded immediately, ind instead delayed based on network connectivity.

For this application, we want to track the driving duration for each trip, and
send a warning to drivers who have driven more than 6 hours in a trip.

## Data

The `vehicle_updates` stream contains sensor data from the vehicles in the
following (CloudEvents) format:

| Field     | Meaning                       |
| --------- | ----------------------------- |
| `type`    | `com.example.car.report`      |
| `source`  | A per-installation unique URL |
| `time`    | The timestamp of the record   |
| `subject` | A per-vehicle unique id       |
| payload   | A JSON record, see below      |

The payload is a json object with at least the following schema (the schema
could be extended later, which is beyond this exercise).

| Field        | Meaning                            |
| ------------ | ---------------------------------- |
| `loc`        | Latitude and longitude measurement |
| `precision`  | meters precision                   |
| `engine_rpm` | engine speed                       |

## Processing

Data processing is performed in two stages:

1. In the first stage, the raw `vehicle_updates` data is transformed by adding
   summary movement deltas between each point and the previous using a fixed
   windowing algorithm. This is implemented in Java with $APPROPRIATE_LIBRARY.
   <!-- TODO: Select library and implement -->

   The output of this stage is written to the `vehicles_cooked` stream, with
   event type `com.example.car.report.processed`.˝

2. Once the deltas have been computed, a second pass is used to determine the
   trips (sessions). A trip is complete when the vehicle has been stopped for
   more than 45 minutes, but we'd like to emit a "possible trip completion" when
   the vehicle has been stopped for more than 2 minutes.
   <!-- TODO: How do we implement this type of session? In particular, what do
   retractions and corrections look like? -->
