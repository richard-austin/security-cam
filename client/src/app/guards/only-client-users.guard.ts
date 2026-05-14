import {inject} from '@angular/core';
import {ActivatedRouteSnapshot, CanActivateFn, Router, RouterStateSnapshot, UrlTree} from '@angular/router';
import {Observable} from 'rxjs';
import {map} from "rxjs/operators";
import {UtilsService} from "../shared/utils.service";

export const OnlyClientUsersGuard: CanActivateFn = (route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<boolean | UrlTree> | Promise<boolean | UrlTree> | boolean | UrlTree => {
  const router: Router = inject(Router);
  return inject(UtilsService).getUserAuthorities().pipe(
    // Map to true if authority is ROLE_CLIENT, or return home url if not
    map((val) =>
      val.find(v => v.authority === 'ROLE_CLIENT') !== undefined
        ? true
        : router.createUrlTree(['/'])
    ));
}
