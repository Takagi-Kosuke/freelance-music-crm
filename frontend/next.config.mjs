/** @type {import('next').NextConfig} */
const nextConfig = {
  // API proxy: ローカルは localhost:8080、本番は NEXT_PUBLIC_API_URL 環境変数を使用
  async rewrites() {
    const backendUrl = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';
    return [
      {
        source: '/api/:path*',
        destination: `${backendUrl}/api/:path*`,
      },
    ];
  },
};

export default nextConfig;
