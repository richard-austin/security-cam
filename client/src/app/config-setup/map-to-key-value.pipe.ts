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
