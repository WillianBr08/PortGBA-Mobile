/// <reference lib="webworker" />
import { buildApk, type BuildRequest } from "./apk";
const scope = self as unknown as DedicatedWorkerGlobalScope;
scope.onmessage = async (event: MessageEvent<BuildRequest>) => {
  try {
    const result = await buildApk(event.data, (value, message) =>
      scope.postMessage({ type: "progress", value, message }),
    );
    scope.postMessage({ type: "done", ...result }, [result.apk.buffer]);
  } catch (error) {
    scope.postMessage({
      type: "error",
      message:
        error instanceof Error
          ? error.message
          : "Não foi possível gerar o APK. Tente novamente.",
    });
  }
};
