import { Injectable } from '@angular/core';
import { User } from '../../shared/models/user.model';
import { Http, RequestOptions, Headers } from '@angular/http';
import { Observable } from 'rxjs';
import 'rxjs/add/operator/map'
import { Campus } from '../../shared/models/campus.model';
import { Location } from '../../shared/models/location.model';

@Injectable()
export class CheckinService {

    private _appUrl = "http://localhost:3000/API"

    constructor(private http: Http) { }

    getLocation(id): Observable<Location> {
        return this.http.get(`/API/location/${id}`)
            .map(res => res.json()).map(item => Location.fromJSON(item));   
        }

    checkIn(userid, locationid): Observable<boolean> {
        let headers = new Headers({ 'Content-Type': 'application/json' });
        let options = new RequestOptions({ headers: headers });
        return this.http.post('/API/checkin/', JSON.stringify({userid: userid, locationid: locationid}), options)
            .map(res => res.json()).map(item => {
                console.log(item);
                return true;
            });
    }

}
