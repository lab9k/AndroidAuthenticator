import { Injectable } from '@angular/core';
import { User } from '../../shared/models/user.model';
import { Http, RequestOptions, Headers } from '@angular/http';
import { Observable, BehaviorSubject } from 'rxjs';
import 'rxjs/add/operator/map'

declare const gapi: any;

@Injectable()
export class AuthenticationService {

  private _appUrl = "http://localhost:3000/API"
  private _user: BehaviorSubject<string>;
  public auth2: any;
  public redirectUrl: string;

  constructor(private http: Http) {
    const currentUser = JSON.parse(localStorage.getItem('currentUser'));
    this._user = new BehaviorSubject<string>(
      currentUser && currentUser.email);
   }

  login(token) : Observable<boolean> {
    let headers = new Headers({ 'Content-Type': 'application/json' });
    let options = new RequestOptions({ headers: headers });
    return this.http.post('/API/login/', JSON.stringify({token: token}), options)
        .map(response => response.json()).map(res => {
            if(res) {
                localStorage.setItem('currentUser',
                    JSON.stringify({ email: res._id, name: res.name, picture: res.picture}));
                this._user.next(res._id);
                return true;
            }
            return false;
        });
  }

  logout() {
    if (this.user.getValue()) {  
        localStorage.removeItem('currentUser');
        setTimeout(() => this._user.next(null));
    }
    gapi.load('auth2', () => {
        this.auth2 = gapi.auth2.init({
          client_id: '780736623262-jcskkstckghd9fg2nom07dgq393ttehp.apps.googleusercontent.com',
          cookiepolicy: 'single_host_origin',
          scope: 'profile email'
        });
        this.auth2.signOut();
      });
  }

  get user() : BehaviorSubject<string> {
      return this._user;
  }

}
