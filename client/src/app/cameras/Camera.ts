
export enum uriType {lo="lo", hd="hd"};

export class Camera
{
    name: string = "";
    uris: {type:uriType, uri:string}[] = [];
    recordings: {type:uriType, uri:string}[] = [];
}
