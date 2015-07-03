var mongo = require("../app/models/mongo.js");

function loginSocketIO(data, callback)
{
  var array = data.split("|");
  console.log("USERNAME ", array[0]);
  console.log("PASSWORD HASH ", array[1]);
  var user = array[0];
  var pass = array[1];
  mongo.databaseLogin(user, pass, function(err, response)
  {
    if(err)
    {
      callback (err, null);
    }
    else {
      callback(null, user);
    }
  });
}

function signupSocketIO(data, callback)
{
    var array = data.split("|");
    console.log("USERNAME ", array[0]);
    console.log("PASSWORD HASH ", array[1]);
    var user = array[0];
    var pass = array[1];
    mongo.databaseSignup(user, pass, function(err, response)
    {
      if(err)
      {
        callback (err, null);
      }
      else {
        callback(null, user);
      }
    });
}

module.exports.loginSocketIO = loginSocketIO;
module.exports.signupSocketIO = signupSocketIO;
