import { Component, OnInit } from '@angular/core';
import { Location } from '../../../../shared/models/location.model';
import { CheckinService } from '../../checkin.service';
import { ActivatedRoute } from '@angular/router';
import { AuthenticationService } from '../../../../shared/services/authentication.service';
import { Observable } from 'rxjs';

declare var $: any;

@Component({
  selector: 'app-location',
  templateUrl: './location.component.html',
  styleUrls: ['./location.component.css']
})
export class LocationComponent implements OnInit {

  private _location: Location;
  private locationName: string;

  constructor(private route: ActivatedRoute, private checkinService: CheckinService, private authService: AuthenticationService) { }

  ngOnInit() {
    this.route.data.subscribe(item => {
      this._location = item['location'];
      if(this._location) {
        this.currentUser.subscribe(user => {
          this.checkinService.checkIn(this.authService.user.getValue() , this._location.id)
            .subscribe();
        });
      }
    });
  }

  nameLoc() {
    this.locationName = $("input[name='location-name']").val();
    if(this.locationName.trim() !== "") {
      this.checkinService.nameLocation(this.locationName, this.location.id).subscribe();
    }

  }

  get currentUser(): Observable<string> {
    return this.authService.user;
  } 

  get location() {
    return this._location;
  }

}
