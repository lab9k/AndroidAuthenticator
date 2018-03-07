var express = require('express');
var router = express.Router();
let mongoose = require('mongoose');

let Location = mongoose.model('Location');
let User = mongoose.model('User');
let Campus = mongoose.model('Campus');
let Segment = mongoose.model('Segment');

const {OAuth2Client} = require('google-auth-library');
const client = new OAuth2Client('780736623262-jcskkstckghd9fg2nom07dgq393ttehp.apps.googleusercontent.com');

/* GET home page. */
router.get('/', function(req, res, next) {
  res.render('index', { title: 'Express' });
});

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
      
      Location.findById(req.body.locationid, function(err, location) {
        if (err) { return next(err);}
        if(!location) {
          return res.status(200).json({message: 'New sticker'});
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

/* CREATE USER */
router.post('/API/user/', function(req, res, next) {
  let user = new User(req.body);
  user.save(function(err, usr) {
    if (err) { return next(err);}
    res.json(usr);
  })
});

/* CREATE CAMPUS */
router.post('/API/campus/', function(req, res, next) {
  let campus = new Campus(req.body);
  campus.save(function(err, camp) {
    if(err) { return next(err);}
    res.json(camp);
  })
});

/* CREATE SEGMENT */
router.post('/API/segment/', function(req, res, next) {
  let segment = new Segment(req.body);
  segment.save(function(err, seg) {
    if(err) { return next(err);}
    res.json(seg);
  })
});

/* CREATE LOCATION */
router.post('/API/location/', function(req, res, next) {
  let location = new Location(req.body);
  location.save(function(err, loc) {
    if(err) { return next(err);}
    res.json(loc);
  })
});

/* UPDATE USER */
router.put('/API/user/', function(req, res, next) {
  User.findByIdAndUpdate(req.body._id, req.body, function (err, user) {
    if (err) { return next(err); }
    res.json(user);
  })
});

/* UPDATE CAMPUS */
router.put('/API/campus/', function(req, res, next) {
  Campus.findByIdAndUpdate(req.body._id, req.body, function (err, campus) {
    if (err) { return next(err); }
    res.json(campus);
  })
});

/* UPDATE SEGMENT */
router.put('/API/segment/', function(req, res, next) {
  Segment.findByIdAndUpdate(req.body._id, req.body, function (err, segment) {
    if (err) { return next(err); }
    res.json(segment);
  })
});

/* UPDATE LOCATION */
router.put('/API/location/', function(req, res, next) {
  console.log(req.body);
  Location.findByIdAndUpdate(req.body._id, req.body, function (err, location) {
    if (err) { return next(err); }
    res.json(location);
  })
});

/* GET CAMPUSES */
router.get('/API/campuses', function(req, res, next) {
  Campus.aggregate([
    {$unwind: {"path": "$segments", "preserveNullAndEmptyArrays": true}},
    {$lookup:
      {
        from: "segments",
        localField: "segments",
        foreignField: "_id",
        as: "segment"
      }
    },
    {$lookup:
      {
        from: "locations",
        localField: "segment.locations",
        foreignField: "_id",
        as: "locations"
      }
    },
    {
      $group: {
        _id: "$_id",
        name: {$first: "$name"},
        segments: { $push: {
          _id: {$arrayElemAt:["$segment._id", 0]},
          name: {$arrayElemAt:["$segment.name", 0]},
          locations: "$locations"}
        },
      }
    },
  ],function(err, campuses) {
    if(err) {next(err);}
    
    res.json(campuses);
  });
});

/* GET SEGMENTS */
router.get('/API/segments', function(req, res, next) {
  Segment.aggregate([
    {$lookup:
      {
        from: "locations",
        localField: "locations",
        foreignField: "_id",
        as: "locations"
      }
    }
  ],function(err, segments) {
    if(err) {next(err);}
    
    res.json(segments);
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

/* GET USER BY ID */
router.get('/API/user/:id', function(req, res, next) {
  User.findById(req.params.id, function(err, user) {
    if (err) { return next(err); } 
    if(user) {
      res.json(user);
    }
    else {
      return res.status(400).json({message: 'User does not exist'});
    } 
  });
});

/* GET CAMPUS BY ID */
router.get('/API/campus/:id', function(req, res, next) {
  Campus.aggregate([
    {$match: {_id: req.params.id}},
    {$unwind: {"path": "$segments", "preserveNullAndEmptyArrays": true}},
    {$lookup:
      {
        from: "segments",
        localField: "segments",
        foreignField: "_id",
        as: "segment"
      }
    },
    {$lookup:
      {
        from: "locations",
        localField: "segment.locations",
        foreignField: "_id",
        as: "locations"
      }
    },
    {
      $group: {
        _id: "$_id",
        name: {$first: "$name"},
        segments: { $push: {
          _id: {$arrayElemAt:["$segment._id", 0]},
          name: {$arrayElemAt:["$segment.name", 0]},
          locations: "$locations"}
        },
      }
    },
  ],function(err, campus) {
    if (err) {next(err);}
    if (campus) {
      res.json(campus);
    } else {
      res.status(400).json({message: "Campus not found."});
    } 
  });
});

/* GET SEGMENT BY ID */
router.get('/API/segment/:id', function(req, res, next) {
  Segment.aggregate([
    {$match: {name: req.params.name}},
    {$lookup:
      {
        from: "locations",
        localField: "locations",
        foreignField: "_id",
        as: "locations"
      }
    }
  ],function(err, segment) {
    if (err) {next(err);}
    if (segment) {
      res.json(segment);
    } else {
      res.status(400).json({message: "Segment not found."});
    } 
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

/* GET USER BY NAME */
router.get('/API/user/name/:name', function(req, res, next) {
  User.find({name: req.params.name}, function(err, user) {
    if (err) { return next(err); }
    if (user[0]) {
      res.json(user[0]);
    } else {
      res.status(400).json({message: "User not found."});
    }
  });
});

/* GET CAMPUS BY NAME */
router.get('/API/campus/name/:name', function(req, res, next) {
  Campus.aggregate([
    {$match: {name: req.params.name}},
    {$unwind: {"path": "$segments", "preserveNullAndEmptyArrays": true}},
    {$lookup:
      {
        from: "segments",
        localField: "segments",
        foreignField: "_id",
        as: "segment"
      }
    },
    {$lookup:
      {
        from: "locations",
        localField: "segment.locations",
        foreignField: "_id",
        as: "locations"
      }
    },
    {
      $group: {
        _id: "$_id",
        name: {$first: "$name"},
        segments: { $push: {
          _id: {$arrayElemAt:["$segment._id", 0]},
          name: {$arrayElemAt:["$segment.name", 0]},
          locations: "$locations"}
        },
      }
    },
  ],function(err, campus) {
    if (err) {next(err);}
    if (campus) {
      res.json(campus);
    } else {
      res.status(400).json({message: "Campus not found."});
    } 
  });
});

/* GET SEGMENT BY NAME */
router.get('/API/segment/name/:name', function(req, res, next) {
  Segment.aggregate([
    {$match: {name: req.params.name}},
    {$lookup:
      {
        from: "locations",
        localField: "locations",
        foreignField: "_id",
        as: "locations"
      }
    }
  ],function(err, segment) {
    if (err) {next(err);}
    if (segment) {
      res.json(segment);
    } else {
      res.status(400).json({message: "Segment not found."});
    } 
  });
});

/* GET LOCATION BY NAME */
router.get('/API/location/name/:name', function(req, res, next) {
  Location.find({name: req.params.name}, function(err, location) {
    if (err) { return next(err); }
    if (location[0]) {
      res.json(location[0]);
    } else {
      res.status(400).json({message: "Location not found."});
    }
  });
});

/* GET LOCATION WITH STICKER */
router.get('/API/location/sticker/:sticker', function(req, res, next) {
  Location.find({stickers: {"$in": [req.params.sticker]}}, function(err, location) {
    if (err) { return next(err); }
    if (location) {
      res.json(location);
    } else {
      res.status(400).json({message: "Location not found."});
    }
  });
});

/* LOGIN */
router.post('/API/login', function(req, res, next) {
  let token = req.body.token;
  verify(token, function(payload) {
    User.findById({_id: payload.email}, function(err, user) {
      if(err) { return next(err); }
      if(user !== null) {
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