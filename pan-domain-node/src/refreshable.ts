export abstract class Refreshable<T> {
  value?: Promise<T>;
  updateTimer?: NodeJS.Timeout;

  cacheTime: number;

  constructor(cacheTime: number) {
    this.cacheTime = cacheTime;

    this.updateTimer = setInterval(() => {
      this.refresh().then(result => {
        this.value = Promise.resolve(result);
      });
    }, this.cacheTime);
  }

  abstract refresh(): Promise<T>;

  get(): Promise<T> {
    if (this.value)
      return this.value;

    this.value = this.refresh();
    return this.value;
  }
}
