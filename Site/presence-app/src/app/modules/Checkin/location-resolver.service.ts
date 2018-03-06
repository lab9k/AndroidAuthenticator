import { Observable } from 'rxjs/Rx';
import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, Resolve, RouterStateSnapshot } from '@angular/router';
import { CheckinService } from './checkin.service';
import { Location } from '../../shared/models/location.model';

@Injectable()
export class LocationResolver implements Resolve<Location> {
    private _location: Location;
    constructor(private checkinService: CheckinService) {}

    resolve(route: ActivatedRouteSnapshot,
            state: RouterStateSnapshot): Observable<Location> {
                
                return this.checkinService.getLocation(route.params['id'])
                .map(item => {
                    if (item) {
                        return item;
                    }
                    console.log(`Item was not found:}`);
                    return null;
                })
                .catch(error => {
                    console.log(`Retrieval error: ${error}`);
                    return Observable.of(new Location(route.params['id'], null));
                });    
            }
 }