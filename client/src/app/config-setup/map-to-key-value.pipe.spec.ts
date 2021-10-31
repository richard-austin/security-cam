import { MapToKeyValuePipe } from './map-to-key-value.pipe';

describe('MapToKeyValuePipe', () => {
  it('create an instance', () => {
    const pipe = new MapToKeyValuePipe();
    expect(pipe).toBeTruthy();
  });
});
