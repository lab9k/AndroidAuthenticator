import { Injectable } from '@angular/core';
import { User } from '../../shared/models/user.model';
import { Http, RequestOptions, Headers } from '@angular/http';
import { HttpErrorResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import 'rxjs/add/operator/map'
import { Campus } from '../../shared/models/campus.model';
import { Location } from '../../shared/models/location.model';
import 'rxjs/add/operator/catch';

@Injectable()
export class CheckinService {

    private _appUrl = "http://localhost:3000/API"

    constructor(private http: Http) { }

    getLocation(id): Observable<Location> {
        return this.http.get(`/API/location/${id}`)
        .map(res => res.json()).map(item => Location.fromJSON(item))
        .catch(this.handleError('getLocation'));
        }

    checkIn(userid, locationid): Observable<string> {
        let headers = new Headers({ 'Content-Type': 'application/json' });
        let options = new RequestOptions({ headers: headers });
        return this.http.post('/API/checkin/', JSON.stringify({userid: userid, locationid: locationid}), options)
            .map(res => {
                return res.statusText;
            });
    }

    nameLocation(name, id): Observable<Location> {
        let headers = new Headers({ 'Content-Type': 'application/json' });
        let options = new RequestOptions({ headers: headers });
        return this.http.put(`/API/location/`, JSON.stringify({name: name, _id:id}), options)
          .map(res => res.json()).map(item => Location.fromJSON(item));
    }

    private handleError(operation: String) {
        return (err: any) => {
            let errMsg = `error in ${operation}() retrieving ${this._appUrl}`;
            console.log(`${errMsg}:`, err)
            if(err instanceof HttpErrorResponse) {
                // you could extract more info about the error if you want, e.g.:
                console.log(`status: ${err.status}, ${err.statusText}`);
                // errMsg = ...
            }
            return Observable.throw(errMsg);
        }
    }
}
