import { KeyValue } from '@angular/common';
import { Pipe, PipeTransform } from '@angular/core';
import { Camera } from '../cameras/Camera';

class KV implements KeyValue<string, any>
{
  constructor(key:string, value:any)
  {
    this.key = key;
    this.value = value;
  }
  key!: string;
  value: any;
}

/**
 * MapToKeyValuePipe: Used in place of the keyvalue pipe to perform the same function without changing the sort order.
 *                    If the sort order is changed by the pipe, the screen form controls will not correspond to the
 *                    form control objects in camControls and streamControls.
 */
@Pipe({
  name: 'mapToKeyValue'
})
export class MapToKeyValuePipe implements PipeTransform {

  transform(items: Map<string, any>, ...args: unknown[]): KeyValue<string, any>[] {
    let retVal:KeyValue<string, any>[] = [];
    items.forEach((value:Camera, key:string) => {
      retVal.push(new KV(key, value));
    })
    return retVal;
  }
}
