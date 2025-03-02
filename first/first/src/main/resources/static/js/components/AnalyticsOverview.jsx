import React, { useState, useEffect } from 'react';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';
import { Card, CardContent } from '@/components/ui/card';
import { TrendingUp, TrendingDown, Activity } from 'lucide-react';

const AnalyticsOverview = () => {
    const [data, setData] = useState(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        fetchData();
    }, []);

    const fetchData = async () => {
        try {
            const response = await fetch('/articles/api/articles/analytics/trends');
            const result = await response.json();
            setData(result);
        } catch (error) {
            console.error('Error fetching analytics:', error);
        } finally {
            setLoading(false);
        }
    };

    const getTrendIndicator = (current, previous) => {
        const percentage = ((current - previous) / previous * 100).toFixed(1);
        const isUp = current > previous;

        return (
            <div className="flex items-center gap-1">
                {isUp ?
                    <TrendingUp className="w-4 h-4 text-green-500" /> :
                    <TrendingDown className="w-4 h-4 text-red-500" />
                }
                <span className={`text-sm ${isUp ? 'text-green-500' : 'text-red-500'}`}>
          {Math.abs(percentage)}%
        </span>
            </div>
        );
    };

    if (loading) {
        return (
            <div className="flex justify-center items-center h-64">
                <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary"></div>
            </div>
        );
    }

    // Transform data for chart
    const monthlyTrends = Object.entries(data?.monthlyStats || {}).map(([month, stats]) => ({
        month,
        articles: stats.articles || 0,
        published: stats.approved || 0,
        rejected: stats.rejected || 0
    }));

    const metrics = data?.metrics || {
        approvalRate: { current: 0, previous: 0 },
        avgReviewTime: { current: 0, previous: 0 },
        activeAuthors: { current: 0, previous: 0 }
    };

    return (
        <div className="space-y-6">
            {/* Monthly Trends Chart */}
            <Card>
                <CardContent className="pt-6">
                    <div className="flex items-center justify-between mb-4">
                        <h3 className="text-lg font-medium">Monthly Trends</h3>
                        <div className="flex items-center gap-4">
                            <div className="flex items-center gap-2">
                                <div className="w-3 h-3 bg-blue-500 rounded"></div>
                                <span className="text-sm text-gray-600">Total</span>
                            </div>
                            <div className="flex items-center gap-2">
                                <div className="w-3 h-3 bg-green-500 rounded"></div>
                                <span className="text-sm text-gray-600">Published</span>
                            </div>
                            <div className="flex items-center gap-2">
                                <div className="w-3 h-3 bg-red-500 rounded"></div>
                                <span className="text-sm text-gray-600">Rejected</span>
                            </div>
                        </div>
                    </div>
                    <div className="h-72">
                        <ResponsiveContainer width="100%" height="100%">
                            <BarChart data={monthlyTrends}>
                                <CartesianGrid strokeDasharray="3 3" />
                                <XAxis dataKey="month" />
                                <YAxis />
                                <Tooltip />
                                <Bar dataKey="articles" fill="#3b82f6" name="Total Articles" />
                                <Bar dataKey="published" fill="#22c55e" name="Published" />
                                <Bar dataKey="rejected" fill="#ef4444" name="Rejected" />
                            </BarChart>
                        </ResponsiveContainer>
                    </div>
                </CardContent>
            </Card>

            {/* Key Metrics */}
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                <Card>
                    <CardContent className="pt-6">
                        <div className="flex items-center justify-between">
                            <div>
                                <p className="text-sm text-gray-500">Approval Rate</p>
                                <h4 className="text-2xl font-bold">
                                    {metrics.approvalRate.current.toFixed(1)}%
                                </h4>
                                {getTrendIndicator(
                                    metrics.approvalRate.current,
                                    metrics.approvalRate.previous
                                )}
                            </div>
                            <Activity className="w-8 h-8 text-blue-500" />
                        </div>
                    </CardContent>
                </Card>

                <Card>
                    <CardContent className="pt-6">
                        <div className="flex items-center justify-between">
                            <div>
                                <p className="text-sm text-gray-500">Avg. Review Time</p>
                                <h4 className="text-2xl font-bold">
                                    {metrics.avgReviewTime.current.toFixed(1)} days
                                </h4>
                                {getTrendIndicator(
                                    metrics.avgReviewTime.current,
                                    metrics.avgReviewTime.previous
                                )}
                            </div>
                            <Activity className="w-8 h-8 text-purple-500" />
                        </div>
                    </CardContent>
                </Card>

                <Card>
                    <CardContent className="pt-6">
                        <div className="flex items-center justify-between">
                            <div>
                                <p className="text-sm text-gray-500">Active Authors</p>
                                <h4 className="text-2xl font-bold">
                                    {metrics.activeAuthors.current}
                                </h4>
                                {getTrendIndicator(
                                    metrics.activeAuthors.current,
                                    metrics.activeAuthors.previous
                                )}
                            </div>
                            <Activity className="w-8 h-8 text-green-500" />
                        </div>
                    </CardContent>
                </Card>
            </div>
        </div>
    );
};

export default AnalyticsOverview;