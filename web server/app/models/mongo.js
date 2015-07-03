var User = require("./user");

function databaseLogin(email, pass, callback)
{
  User.findOne({'local.email' : email}, function(err, user)
  {
      if(err)
      {
        callback(err, null);
      }
      else if(!user)
      {
        callback("noSuchUser", null);
      }
      else if(!user.validPassword(pass))
      {
        callback("badPass", null);
      }
      else
      {
        callback(null, "success");
      }
  });
}

function databaseSignup(email, pass, callback)
{
  User.findOne({'local.email' : email}, function(err, user)
  {
      if(err)
      {
        callback(err, null);
      }
      else {
        if(user){
          callback("existentUser", null);
        }
        else {
          var newUser = new User();
          newUser.local.email = email;
          newUser.local.password = newUser.pass;
  //pass hash is already generated
  //this might need extra protection
  //like hashing the whole thing

          //write to DB
          newUser.save(function(err)
          {
            if(err)
            {
              callback(err, null);
            }
            else {
              callback(null, "success");
            }
          });
        }
      }
  });
}

function updateDeviceList(user, deviceID, deviceName, callback)
{
  User.update({ 'local.email' : user},
  { $push: {'devices' : {'id': deviceID, 'name':deviceName, 'agenda' : "", 'battery' : 0}}},
  function(err, response)
  {
    if(err)
    {
      console.log("Update device error", err);
      callback(err);
    }
    else {
      console.log("Update device OK");
      callback(null);
    }
  });
}

function removeDevice(user, deviceID)
{
  User.update({'local.email' : user, 'devices.id' : deviceID},
  {$pull : {'devices' :  { 'id' : deviceID}}},
  function(err, respose)
  {
    if(err)
    {
      console.log("Update device error", err);
    }
    else {
      console.log("Update device OK");
    }
  })
}

function updateAgenda(user, deviceID, agenda)
{
  User.findOneAndUpdate({'local.email' : user, 'devices.id' : deviceID},
  {$set: {"devices.$.agenda" : agenda}},
  {   select: {
            'devices': {
               $elemMatch:
               {   'id' : deviceID }
            }
        }
  },
  function(err, response)
  {
    if(err)
    {
      console.log("Update agenda error", err);
    }
    else {
      console.log("Updated agenda OK");
    }
  });
}

function updateBatteryLevel(user, deviceID, value)
{
  User.findOneAndUpdate({'local.email' : user, 'devices.id' : deviceID},
  {$set: {"devices.$.battery" : value}},
  {
    select :{
      'devices' : {
        $elemMatch :
        {
          'id' : deviceID
        }
      }
    }
  },
    function(err, response)
    {
      if(err)
      {
        console.log("Update battery error", err);
      }
      else {
        console.log("Update battery OK");
      }
    })
}

function retrieveBatteryLevel(user, deviceID)
{
  User.findOne({'local.email' : user, 'devices.id' : deviceID},
  function(err, foundUser)
  {
    if(err)
    {
      console.log("Retrieve battery error", err);
    }
    else {
      console.log("found user is: ", foundUser);
    }
  });
}

/* NOTIFICATIONS FUNCTIONS */

function removeAllNotif(user, deviceID)
{
  User.update({'local.email' : user, 'notifications.id' : deviceID},
  {$pull : {'notifications' :  { 'id' : deviceID}}},
  function(err, respose)
  {
    if(err)
    {
      console.log("Remove settings error", err);
    }
    else {
      console.log("Remove settings OK");
    }
  });
}

function removeNotif(user, deviceID, notifID)
{
  User.update({'local.email' : user, 'notifications.notification' : notifID},
  {$pull : {'notifications' :  { 'notification' : notifID}}},
  function(err, response)
  {
    if(err)
    {
      console.log("Update notification error", err);
    }
    else {
      console.log("Update notification OK");
    }
  })
}

function fetchNotifications(user, deviceID, callback)
{
  User.find({'local.email': user,'notifications.id': deviceID},
  function(err, result)
  {
    if(err)
    {
      console.log("Fetch notifications ERR" ,err)
    }
    else {
      console.log("Fetch notifications OK");
      console.log(result);
      var json = result[0];
      if(json == undefined | json == null)
      {
        callback([]);
      }
      else {
        console.log("JSON notif", json['notifications']);
        callback(json['notifications']);
      }
    }
  })
}

function addNotification(user, deviceID, notification)
{
  User.update({ 'local.email' : user},
  { $push: {'notifications' : {'id': deviceID, 'notification': notification}}},
  function(err, response)
  {
    if(err)
    {
      console.log("Update notification error", err);
    }
    else {
      console.log("Update notification OK");
    }
  });
}

/* CALL LOG FUNCTIONS */

function removeLogEntry(user, deviceID, log)
{
  User.update({'local.email' : user, 'calls.log' : log.number, 'calls.timestamp' : log.timestamp},
  {$pull : {'calls' :  { 'log' : log.number, 'timestamp' : log.timestamp}}},
  function(err, response)
  {
    if(err)
    {
      console.log("Update call log error", err);
    }
    else {
      console.log("Update call log OK");
      console.log("RESP", response);
    }
  })
}

function addLogEntry(user, deviceID, log)
{
  User.update({ 'local.email' : user},
  { $push: {'calls' : {'id': deviceID, 'log': log.number, 'timestamp' : log.timestamp}}},
  function(err, response)
  {
    if(err)
    {
      console.log("Update log error", err);
    }
    else {
      console.log("Update log OK");
    }
  });
}

function fetchCallLog(user, deviceID, callback)
{
  User.find({'local.email': user,'calls.id': deviceID},
  function(err, result)
  {
    if(err)
    {
      console.log("Fetch call log ERR" ,err)
    }
    else {
      console.log("Fetch call log OK");
      var json = result[0];
      if(json == undefined | json == null)
      {
        callback([]);
      }
      else {
        console.log("JSON call log", json['calls']);
        callback(json['calls']);
      }
    }
  });
}

function removeAllLog(user, deviceID)
{
  User.update({'local.email' : user, 'calls.id' : deviceID},
  {$pull : {'calls' :  { 'id' : deviceID}}},
  function(err, respose)
  {
    if(err)
    {
      console.log("Remove logs error", err);
    }
    else {
      console.log("Remove logs OK");
      console.log("REZZZ", respose);
    }
  });
}

/* SETTINGS FUNCTIONS */
function retrieveSettings(user, deviceID, callback)
{
  User.findOne({'local.email' : user, 'settings.id' : deviceID},
  {
      'settings': {
         $elemMatch:
         {   'id' : deviceID }
      }
  },
  function(err, result)
  {
    if(err)
    {
      console.log("Mongo retrieveSettings ERR", err);
    }
    else {
      console.log("Fetch settings OK");
      if(result != null && result != undefined)
      {
        callback(result['settings'][0]['settings']);
      }
      else {
        callback({});
      }
    }
  });
}

function insertSettings(user, deviceID, settings)
{
  User.update({ 'local.email' : user},
  { $push: {'settings' : {'id': deviceID, 'settings': settings}}},
  function(err, response)
  {
    if(err)
    {
      console.log("Insert settings error", err);
    }
    else {
      console.log("Insert settings OK");
    }
  });
}

function updateSettings(user, deviceID, settings)
{
  User.findOneAndUpdate({'local.email' : user, 'settings.id' : deviceID},
  {$set: {"settings.$.settings" : JSON.stringify(settings)}},
  function(err, response)
  {
    if(err)
    {
      console.log("Update settings error", err);
    }
    else {
      console.log("Updated settings OK");
    }
  });
}

function removeSettings(user, deviceID)
{
  User.update({'local.email' : user, 'settings.id' : deviceID},
  {$pull : {'settings' :  { 'id' : deviceID}}},
  function(err, respose)
  {
    if(err)
    {
      console.log("Remove settings error", err);
    }
    else {
      console.log("Remove settings OK");
    }
  })
}

module.exports.databaseLogin = databaseLogin;
module.exports.databaseSignup = databaseSignup;
module.exports.updateDeviceList = updateDeviceList;
module.exports.updateAgenda = updateAgenda;
module.exports.removeDevice = removeDevice;
module.exports.updateBatteryLevel = updateBatteryLevel;
module.exports.retrieveBatteryLevel =retrieveBatteryLevel; // this is not needed for now
module.exports.removeAllNotif = removeAllNotif;
module.exports.removeNotif = removeNotif;
module.exports.fetchNotifications = fetchNotifications;
module.exports.addNotification = addNotification;
module.exports.addLogEntry = addLogEntry;
module.exports.fetchCallLog = fetchCallLog;
module.exports.removeLogEntry = removeLogEntry;
module.exports.retrieveSettings = retrieveSettings;
module.exports.updateSettings = updateSettings;
module.exports.insertSettings = insertSettings;
module.exports.removeSettings = removeSettings;
module.exports.removeAllLog = removeAllLog;
