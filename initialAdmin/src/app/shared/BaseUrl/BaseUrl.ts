import {Injectable} from '@angular/core';
import {environment} from '../../../environments/environment';


@Injectable()
export class BaseUrl {
    getLink(controller: string, method: string) {
        return environment.baseUrl + controller + (controller !== '' ? '/' : '') + method;
     }
}
