import { Injectable } from '@angular/core';
import {ActivatedRouteSnapshot, CanActivate, RouterStateSnapshot, UrlTree } from '@angular/router';
import { Observable } from 'rxjs';
import {map} from "rxjs/operators";
import { UtilsService } from '../shared/utils.service';

@Injectable({
  providedIn: 'root'
})
export class OnlyClientUsersService implements CanActivate {

  constructor(private utilsService: UtilsService) { }

  canActivate(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<boolean | UrlTree> | Promise<boolean | UrlTree> | boolean | UrlTree {
    return this.utilsService.getUserAuthorities().pipe(
      // Map to true if authority is ROLE_CLIENT
      map((val) =>
        val.find(v => v.authority === 'ROLE_CLIENT') !== undefined
      ));
  }
}
