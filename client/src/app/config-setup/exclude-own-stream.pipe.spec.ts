import { ExcludeOwnStreamPipe } from './exclude-own-stream.pipe';

describe('ExcludeOwnStreamPipe', () => {
  it('create an instance', () => {
    const pipe = new ExcludeOwnStreamPipe();
    expect(pipe).toBeTruthy();
  });
});
