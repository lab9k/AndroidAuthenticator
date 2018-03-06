var express = require('express');
var router = express.Router();
let mongoose = require('mongoose');

let Location = mongoose.model('Location');
let User = mongoose.model('User');
let Campus = mongoose.model('Campus');
const {OAuth2Client} = require('google-auth-library');
const client = new OAuth2Client('780736623262-jcskkstckghd9fg2nom07dgq393ttehp.apps.googleusercontent.com');

String.prototype.toObjectId = function() {
  var ObjectId = (require('mongoose').Types.ObjectId);
  return new ObjectId(this.toString());
};

/* GET home page. */
router.get('/', function(req, res, next) {
  res.render('index', { title: 'Express' });
});
/*
router.get('/API/test', function(req, res, next) {
  Location.aggregate([ 
    {$unwind: "$checkins" },
    {$sort: { "checkins.time": -1}},
    {$group: { 
      _id: "$checkins.userid", 
      name: {$first: "$name"},
      time: {$first: "$checkins.time"}
    }},
    {$lookup: {
      from: "users",
      localField: "_id",
      foreignField: "_id",
      as: "user"
    }}, 
    { $project: {  
      _id: 0,
      user: {$arrayElemAt: ["$user", 0]},
      name: 1,
      time: 1
    }},
    {$group: {
      _id: "$name",
      users: {$push: {user: "$user", time: "$time"}}
    }},
    {$project: {
      _id: 0,
      name: "$_id",
      users: 1
    }}
    
  ], function(err, locations) {
      if(err) { return next(err);}
      res.json(locations);
    });
});
*/
/* Get checkins from all locations from the last 8 hours 
router.get('/API/checkins', function(req, res, next) {
  if((req.query.seconds && isNaN(req.query.seconds)) || (req.query.hours && isNaN(req.query.locationId))) {
    return res.status(400).json(
      {message: 'Enter a number for hours/locationId.'}
    );
  }
  let seconds = 8 * 3600;

  if(req.query.seconds) { 
    seconds = req.query.seconds;
  }
  if(req.query.locationId) {
    Location.aggregate([{$match: {"checkins.time": {$gte: +new Date() - seconds * 1000}, "_id": req.query.locationId}},
                        {$project: {
                          name: 1,
                          checkins: {$filter: {
                            input: "$checkins",
                            as: "checkin",
                            cond: {$gte: ["$$checkin.time", +new Date() - seconds * 1000]}
                          }},
                          _id: 1
                        }}], function(err, locations) {
                              if(err) { return next(err);}
                              res.json(locations);
    });
  }
  else {
    Location.aggregate([{$match: {"checkins.time": {$gte: +new Date() - seconds * 1000}}},
                        {$project: {
                          name: 1,
                          checkins: {$filter: {
                            input: "$checkins",
                            as: "checkin",
                            cond: {$gte: ["$$checkin.time", +new Date() - seconds * 1000]}
                          }},
                          _id: 1
                        }}], function(err, locations) {
                              if(err) { return next(err);}
                              res.json(locations);
    });
  }

});
*/
/* Checkin user */
router.post('/API/checkin/', function(req, res, next) {
  if(!req.body.userid || !req.body.locationid) {
    return res.status(400).json(
      {message: 'Missing fields'}
    );
  }
  let user = null;
  let id = req.body.locationid;
  
  User.findById(req.body.userid, function(err, usr) {
    if(err) {return next(err);}
    if(usr) {
      user = usr;
      
      Location.findById(id, function(err, location) {
        if (err) { return next(err);}
        if(!location) {
          location = new Location({"_id": id, "name": undefined});
          location.save(function(err, loc) {
            if (err) { return next(err);}
            Campus.findById("Not in a campus", function(err, cmp) {
              cmp.locations.push(location);
              cmp.save(function(err, camp) {
                if(err) {return next(err);}
              });
            });
          });
        }
        if(!location.name) {
          user.checkin = { location: location, time: +new Date()};
          location.save(function(err, loc) {
            if (err) { return next(err);}
          });
          user.save(function(er, usr) {
            if(err) { return next(err);}
          });
          return res.status(200).json({message: 'Location has no name.'});
        }
        user.checkin = { location: location, time: +new Date()};
        user.save(function(err, usr) {
          if (err) { return next(err);}
          return res.status(200).json({message: 'Checkin succesful.'});
        });
      });
    }
    else {
      return res.status(400).json({message: 'User does not exist'});
    }  
  });
});

/*Add user */
router.post('/API/user/', function(req, res, next) {
  let user = new User(req.body);
  user.save(function(err, usr) {
    if (err) { return next(err);}
    res.json(usr);
  })
});

/* Name location */
router.put('/API/location/', function(req, res, next) {
  if(!req.body.name) {
    return res.status(400).json(
      {message: 'Enter name.'}
    );
  }
  Location.findById(req.body._id, function(err, location) {
    if(!location.name) {
      location.name = req.body.name;
      location.save(function(err, loc) {
        if (err) { return next(err);}
        res.json(loc);
      });
    }
    else {
      return res.status(400).json(
        {message: 'Location already has a name.'}
      );
    }
  });
})

/* CREATE CAMPUS */
router.post('/API/campus/', function(req, res, next) {
  let campus = new Campus(req.body);
  campus.save(function(err, camp) {
    if(err) { return next(err);}
    res.json(camp);
  })
});

/* ADD LOCATION TO CAMPUS */
router.put('/API/campus/:name', function(req, res, next) {
  console.log("add " + req.params.name + " " + req.body.id);
  Campus.findById(req.params.name, function(err, camp) {
    if(err) { return next(err);}
    Location.findById(req.body.id, function(err, location) {
      console.log(location);
      camp.locations.push(location);
      console.log(camp);
      camp.save(function(err, cam) {
        if(err) {next(err);}
        res.json(cam);
      });
    });
  });
});

/* REMOVE LOCATION FROM CAMPUS */
router.delete('/API/campus/:name/location/:id', function(req, res, next) {
  console.log("remove name:" + req.params.name + " id:" + req.params.id);
  Campus.findById(req.params.name, function(err, camp) {
    if(err) { return next(err);}
    Location.findById(req.params.id, function(err, location) {
      console.log(camp);
      console.log(location);
      let index = camp.locations.indexOf(location._id);
      console.log(index);
      camp.locations.splice(index, 1);
      camp.save(function(err, cam) {
        if(err) {next(err);}
        res.json(cam);
      });
    });
  });
});

/* GET ALL CAMPUS */
router.get('/API/campuses', function(req, res, next) {
  Campus.aggregate([
    {$unwind: {"path": "$locations", "preserveNullAndEmptyArrays": true}},
    {$lookup:
      {
        from: "locations",
        localField: "locations",
        foreignField: "_id",
        as: "location"
      }},
      {
        $group: {
          _id: "$_id",
          locations: {$push: {$arrayElemAt: ["$location", 0]}}
        }
      },
  ],function(err, campuses) {
    if(err) {next(err);}
    
    res.json(campuses);
  });
});

/* GET LOCATIONS */
router.get('/API/locations/', function(req, res, next) {
  Location.find(function(err, locations) {
    if(err) {return next(err);}
    res.json(locations);
  });
});

/* GET USERS */
router.get('/API/users', function(req, res, next) {
  User.find(function(err, users) {
    if (err) { return next(err); }
    res.json(users);
  });
});

/* GET LOCATION BY ID */
router.get('/API/location/:id', function(req, res, next) {
  Location.findById(req.params.id, function(err, location) {
    if (err) { return next(err); } 
    if(location) {
      res.json(location);
    }
    else {
      return res.status(400).json({message: 'Location does not exist'});
    } 
  });
});

router.post('/API/login', function(req, res, next) {
  let token = req.body.token;
  verify(token, function(payload) {
    User.findById({_id: payload.email}, function(err, user) {
      if(err) { return next(err); }
      console.log(user);
      if(user !== []) {
        console.log(user);
        res.json(user);
      }
      else {
        let user = new User({
          _id: payload.email, 
          name: payload.name, 
          checkin: [], 
          picture: payload.picture
        });
        user.save(function(err, usr) {
          if(err) { return next(err);}
          console.log(usr);
          res.json(usr);
        });
      }
    });
  }).catch(console.error);
  
});

async function verify(token, callback) {
  const ticket = await client.verifyIdToken({
      idToken: token,
      audience: '780736623262-jcskkstckghd9fg2nom07dgq393ttehp.apps.googleusercontent.com',  
  });
  const payload = ticket.getPayload();
  const userid = payload['sub'];
  callback(payload);
  // If request specified a G Suite domain:
  //const domain = payload['hd'];
}

module.exports = router;