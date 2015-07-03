// load the things we need
var mongoose = require('mongoose');
var bcrypt   = require('bcrypt-nodejs');

// define the schema for our user model
var userSchema = mongoose.Schema({

    local            : {
        email        : String,
        password     : String,
    },
    devices : [{
      id : String,
      name : String,
      agenda : String,
      battery : Number
    }],
    notifications : [{
      id: String,
      notification: String
    }],
    calls : [{
      id: String,
      log : String,
      timestamp : String
    }],
    settings : [{
      settings : String,
      id : String
    }]
});

// methods ======================
// generating a hash
userSchema.methods.generateHash = function(password) {
    return bcrypt.hashSync(password, bcrypt.genSaltSync(), null);
};

// checking if password is valid
userSchema.methods.validPassword = function(password) {
    return bcrypt.compareSync(password, this.local.password);
};

// create the model for users and expose it to our app
module.exports = mongoose.model('User', userSchema);
