import { Injectable } from '@angular/core';
import { User } from '../../shared/models/user.model';
import { Http, RequestOptions, Headers } from '@angular/http';
import { Observable } from 'rxjs';
import 'rxjs/add/operator/map'
import { Campus } from '../../shared/models/campus.model';
import { Location } from '../../shared/models/location.model';

@Injectable()
export class AdminDataService {

  private _appUrl = "http://localhost:3000/API"

  constructor(private http: Http) { }

  locations() : Observable<Location[]> {
    return this.http.get('/API/locations/')
      .map(response => response.json().map(item => Location.fromJSON(item)));
  }

  campuses() : Observable<Campus[]> {
    return this.http.get('/API/campuses')
      .map(response => response.json().map(item => Campus.fromJSON(item)));
  }

  getLocation(id) : Observable<Location> {
    return this.http.get(`/API/location/${id}`)
      .map(response => response.json()).map(item => Location.fromJSON(item));
  }

  addLocation(name, id) : Observable<Campus> {
    let headers = new Headers({ 'Content-Type': 'application/json' });
    let options = new RequestOptions({ headers: headers });
    return this.http.put(`/API/campus/${name}`, JSON.stringify({id: id}), options)
      .map(res => res.json()).map(item => Campus.fromJSON(item));
  }

  removeLocation(name, id) : Observable<Campus> {
    return this.http.delete(`/API/campus/${name}/location/${id}`)
      .map(res => res.json()).map(item => Campus.fromJSON(item));
  }

  addCampus(campus) : Observable<Campus> {
    let headers = new Headers({ 'Content-Type': 'application/json' });
    let options = new RequestOptions({ headers: headers });
    return this.http.post('/API/campus/', JSON.stringify({_id: campus, locations: []}), options)
      .map(res => res.json()).map(item => Campus.fromJSON(item));
  }
}
