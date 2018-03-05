import { ActivatedRouteSnapshot, CanActivate, Router, RouterStateSnapshot } from '@angular/router';
import { Injectable } from '@angular/core';
import { AuthenticationService } from '../../shared/services/authentication.service';

@Injectable()
export class AuthGuardService implements CanActivate {

  canActivate(route: ActivatedRouteSnapshot,
      state: RouterStateSnapshot): boolean {
        if (this.authService.user.getValue()) {
          return true;
        }
        this.authService.redirectUrl = state.url;
        this.router.navigate(['/login']);
        return false;
      }
      
  constructor(private authService: AuthenticationService, private router: Router) { }

}
