import { Component, OnInit } from '@angular/core';
import { Location } from '../../../../shared/models/location.model';
import { CheckinService } from '../../checkin.service';
import { ActivatedRoute } from '@angular/router';
import { AuthenticationService } from '../../../../shared/services/authentication.service';
import { Observable } from 'rxjs';

@Component({
  selector: 'app-location',
  templateUrl: './location.component.html',
  styleUrls: ['./location.component.css']
})
export class LocationComponent implements OnInit {

  private _location: Location;

  constructor(private route: ActivatedRoute, private checkinService: CheckinService, private authService: AuthenticationService) { }

  ngOnInit() {
    this.route.data.subscribe(item => {
      this._location = item['location'];
      this.currentUser.subscribe(user => {
        this.checkinService.checkIn(this.authService.user.getValue() , this._location.id)
          .subscribe();
      });
    });
  }

  get currentUser(): Observable<string> {
    return this.authService.user;
  } 

  get location() {
    return this._location;
  }

}
