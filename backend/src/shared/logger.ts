/** The slice of pino we use, so services don't depend on Fastify. Never pass PII in `context`. */
export interface Logger {
  info(context: object, message: string): void;
  warn(context: object, message: string): void;
  error(context: object, message: string): void;
}

export const silentLogger: Logger = {
  info: () => {},
  warn: () => {},
  error: () => {},
};
