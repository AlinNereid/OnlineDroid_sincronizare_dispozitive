var express  = require('express');
var app      = express();
var port     = process.env.PORT || 3000;
var mongoose = require('mongoose');
var passport = require('passport');
var flash    = require('connect-flash');
var http = require('http');
var server = app.listen(port);
var io = require('socket.io').listen(server);
var path = require('path');

var morgan       = require('morgan');
var cookieParser = require('cookie-parser');
var bodyParser   = require('body-parser');
var session      = require('express-session');

var configDB = require('./config/database.js');
var IOAuth = require('./config/socketIOAuth.js');
var mongo = require('./app/models/mongo.js');

// configuration ===============================================================
mongoose.connect(configDB.url); // connect to our database

require('./config/passport')(passport); // pass passport for configuration

io.on('connection', function(socket)
{
  globalSocket = socket;
  //socket io from client needs to join his private room
  socket.on('webClientJoined', function(username)
  {
      socket.join(username);
      console.log('socketul asculta in camera lui');

      socket.on('sendSmsTo', function(data)
      {
        console.log(data);
        var obj = {phone : data.contact, message: data.message};
        socket.to(username).emit("smsSend", obj);
      });

      socket.on('webClientGetLocation', function(json)
      {
        socket.to(json.user).emit('fetchLoc',json.device);
      });

      socket.on('removeNotification', function(json)
      {
        if(json.notif == 'all')
        {
          mongo.removeAllNotif(username, json.device);
        }
        else {
          mongo.removeNotif(username, json.device, json.notif);
        }
      });

      socket.on('fetchNotifications', function(device)
      {
        mongo.fetchNotifications(username, device.device, function(array)
        {
          socket.emit('notificationsList', array);
        });
      });

      socket.on('fetchLog', function(device)
      {
        mongo.fetchCallLog(username, device.device, function(array)
        {
          socket.emit('loglist', array);
        });
      });

      socket.on('webClientChangeSettings', function(json)
      {
        mongo.updateSettings(username, json.device, json);
        socket.to(username).emit("androidsyncSettings", json.settings);
      });

      socket.on('removeLogEntry', function(json)
      {
        if(json.number == 'all')
        {
          mongo.removeAllLog(username, json.device);
        }
        else {
          mongo.removeLogEntry(username, json.device, json);
        }
      });

      socket.on('request_settings', function(device)
      {
        mongo.retrieveSettings(username, device.id, function(settingsJson)
        {
          socket.emit('new_settings', settingsJson);
        });
      });

      socket.on('rejectCall', function(number)
      {
        socket.to(username).emit("disconnectCall", number);
      });
  });

  //Android devices sockets
  console.log("a new client connected");
  socket.on("login", function(data)
  {
    console.log("Login info: ", data);
    IOAuth.loginSocketIO(data, function(err, user)
    {
      if(err)
      {
        console.log("ERROR SOCKIO LOGIN ", err);
        socket.emit('loginFailed', {data : err});
      }
      else
      {
        console.log("SOCKIO LOGIN OK");
        socket.emit('loginSucceeded', {data : "ok"});
        socket.join(user);
        var receivedElements = data.split("|");
        mongo.updateDeviceList(user, receivedElements[2], receivedElements[3], function(err)
        {
          if(err)
          {
            console.log("emit error back and kill the socket");
            socket.disconnect();
          }
          else
          {
            socket.on("agenda", function(data)
            {
              socket.to(user).emit('clientUpdateDevice', {'id': receivedElements[2], 'name' : receivedElements[3], 'agenda' : data});
              mongo.updateAgenda(user, receivedElements[2], data);
            });

            socket.on("requestContactLog", function(data)
            {
              console.log("REQUESTED FOR ", data);
            });
          }

          socket.on('smsForward', function(data)
          {
            console.log("AM PRIMIT SMS DIN EXTERIOR", data);
            var json = data.split("|");
            socket.to(user).emit('clientReceivedMessage', {'contact': json[0] ,'message': json[1], 'id': receivedElements[2]});
          });
        });

        socket.on("pushNotification", function(data)
        {
          var array = data.split("|");
          console.log("NOTIFICATION: " + array[0] + " | " + array[1] + " | " + array[2] + "|" + array[3] + "|" + array[4]);
          mongo.addNotification(user, receivedElements[2], array[1] + "|" + array[2] + "|" + array[3] + "|" + array[4]);
          socket.to(user).emit("clientReceivedNotification", data);
        });

        socket.on("geoloc", function(data)
        {
          var array = data.split("|");
          console.log("LOCATION: " + array[0] + " | " + array[1]);
          socket.to(user).emit('clientReceivedLocation', {long: array[0], lat: array[1]});
        });

        socket.on("removeDevice", function(deviceID)
        {
          socket.to(user).emit('clientRemoveDevice', {'id' : deviceID});
          mongo.removeSettings(user, deviceID);
          mongo.removeDevice(user, deviceID);
        });

        socket.on("battery_status", function(data)
        {
          console.log("Battery percent: " + data);
          socket.to(user).emit('clientUpdateBattery', { deviceID : receivedElements[2], battery: data});
          mongo.updateBatteryLevel(user, receivedElements[2], data);
        });

        socket.on("phoneCallStart", function(data)
        {
          var timestamp = new Date().getTime();
          console.log("call received from: " + data);
          socket.to(user).emit('forwardPhonecall', {'device' : receivedElements[2], 'number' : data, 'timestamp' : timestamp});
          mongo.addLogEntry(user, receivedElements[2], {'number' : data, 'timestamp' : timestamp});
          //socket.emit("disconnectCall", data);
        });

        socket.on("phoneCallStop", function(data)
        {
          console.log("call stopped from: " + data);
        });

        socket.on("syncSettings", function(data)
        {
          mongo.insertSettings(user, receivedElements[2], data);
        });

        socket.on("updateSettings", function(data)
        {
          mongo.updateSettings(user, receivedElements[2], data);
          socket.to(user).emit("new_settings", data);
        });

        socket.on('disconnect', function()
        {
          mongo.removeSettings
          mongo.removeDevices(user);
        });
      }
    });

  });

  // socket.on("smsForward", function(data)
  // {
  //   console.log("SMS_FORWARD FIRED", data);
  //   var array = data.split("|");
  //   console.log("Message from: " + array[0] + " : " + array[1]);
  //  socket.emit("smsSend", {phone: "0754587225", message: "TESTIES"});
  // });

  // socket.on("signup", function(data)
  // {
  //   console.log("Signup data: ", data);
  //   IOAuth.signupSocketIO(data, function(err, response)
  //   {
  //     if(err)
  //     {
  //       console.log("ERROR SOCKIO SIGNUP ", err);
  //       socket.emit('signupFailed', {data : err});
  //     }
  //     else {
  //       console.log("SOCKIO SIGNUP OK");
  //       socket.emit('signupSucceeded', {data : "ok"});
  //     }
  //   });
  // });
});

// set up our express application
app.use(morgan('dev')); // log every request to the console
app.use(cookieParser()); // read cookies (needed for auth)
app.use(bodyParser()); // get information from html forms

app.set('view engine', 'ejs'); // set up ejs for templating

// required for passport
app.use(session({ secret: 'opendroid' })); // session secret
app.use(passport.initialize());
app.use(passport.session()); // persistent login sessions
app.use(flash()); // use connect-flash for flash messages stored in session

app.use(express.static(__dirname + '/public'));

// routes ======================================================================
require('./app/routes.js')(app, passport); // load our routes and pass in our app and fully configured passport

// launch ======================================================================
// app.listen(port);
console.log('The magic happens on port ' + port);
