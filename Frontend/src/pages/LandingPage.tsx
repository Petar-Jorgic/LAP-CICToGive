import { Navigate } from "react-router";
import { useAuth } from "../hooks/useAuth.tsx";

export default function LandingPage() {
  const { isAuthenticated, isLoading, login } = useAuth();

  if (isLoading) {
    return (
      <div className="min-h-screen bg-base-200 flex items-center justify-center">
        <span className="loading loading-spinner loading-lg"></span>
      </div>
    );
  }

  if (isAuthenticated) {
    return <Navigate to="/home" replace />;
  }

  return (
    <div className="min-h-screen bg-base-200 py-12 px-4 mx-auto">
      <div className="w-full max-w-7xl mx-auto">
        <div className="text-center mb-12">
          <article className="prose lg:prose-xl max-w-none">
            <h1 className="text-4xl font-bold text-base-content mb-4">
              Willkommen bei CIC ToGive
            </h1>
            <p className="text-lg text-base-content/70 max-w-2xl mx-auto">
              Entdecken Sie fantastische Inhalte, durchstöbern Sie unsere
              Sammlungen und finden Sie die perfekten Geschenke. Melden Sie sich
              mit Ihrem IBM W3ID an, um zu beginnen.
            </p>
          </article>
        </div>

        <div className="flex justify-center">
          <button className="btn btn-primary btn-lg text-xl" onClick={login}>
            Mit W3ID anmelden
          </button>
        </div>

        <div className="text-center mt-16">
          <article className="prose prose-sm max-w-none">
            <p className="text-base-content/60">Copyright IBM CIC</p>
          </article>
        </div>
      </div>
    </div>
  );
}
