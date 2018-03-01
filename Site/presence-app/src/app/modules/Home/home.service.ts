import { Injectable } from '@angular/core';
import { User } from '../../shared/models/user.model';
import { Http } from '@angular/http';
import { Observable } from 'rxjs';
import 'rxjs/add/operator/map'
import { Campus } from '../../shared/models/campus.model';

@Injectable()
export class HomeDataService {

  private _appUrl = "http://localhost:3000/API"

  constructor(private http: Http) { }

  users() : Observable<User[]> {
    return this.http.get('/API/users/')
      .map(response => response.json().map(item => User.fromJSON(item)));
  }

  campuses() : Observable<Campus[]> {
    return this.http.get('/API/campuses')
      .map(response => response.json().map(item => Campus.fromJSON(item)));
  }
}
