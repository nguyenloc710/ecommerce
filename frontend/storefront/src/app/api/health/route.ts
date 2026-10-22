/** Probe của Kubernetes (startup/liveness/readiness): chỉ báo tiến trình Next còn phục vụ được, không gọi backend. */
export function GET() {
  return Response.json({ status: "UP" });
}
