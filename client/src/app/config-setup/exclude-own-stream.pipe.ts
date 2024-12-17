import { Pipe, PipeTransform } from '@angular/core';
import {Stream} from "../cameras/Camera";
import {KeyValue} from "@angular/common";

@Pipe({
    name: 'excludeOwnStream',
    standalone: false
})
export class ExcludeOwnStreamPipe implements PipeTransform {
  transform(streams: Array<KeyValue<string, Stream>>, excludeStream: string): Array<KeyValue<string, Stream>> {
    return streams.filter((kv) => kv.key != excludeStream);
  }
}
