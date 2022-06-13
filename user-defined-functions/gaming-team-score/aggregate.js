const udf = require('./streaming-aggregator');

// --------- UDF aggregation function --------
function aggregate(headers, user, results) {

  if (!results.has(user.team)) {
    // Add new empty team to the result map
    results.set(user.team, {
      from: headers.windowStartTime,
      to: headers.windowEndTime,
      team: user.team,
      totalScore: 0,
    });
  }

  // Increment team's score.
  let team = results.get(user.team);

  team.totalScore =
    Number.parseInt(team.totalScore) + Number.parseInt(user.userTotalScore);
}

new udf.Aggregator(aggregate, 55554).start();