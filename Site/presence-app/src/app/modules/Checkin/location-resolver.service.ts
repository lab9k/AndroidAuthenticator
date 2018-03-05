import { Observable } from 'rxjs/Rx';
import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, Resolve, RouterStateSnapshot } from '@angular/router';
import { CheckinService } from './checkin.service';
import { Location } from '../../shared/models/location.model';

@Injectable()
export class LocationResolver implements Resolve<Location> {
    constructor(private checkinService: CheckinService) {}

    resolve(route: ActivatedRouteSnapshot,
            state: RouterStateSnapshot): Observable<Location> {
                return this.checkinService.getLocation(route.params['id']);
            }
 }